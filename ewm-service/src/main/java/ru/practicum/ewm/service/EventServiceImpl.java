package ru.practicum.ewm.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.error.EventDateValidationException;
import ru.practicum.ewm.error.ForbiddenException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.mapper.LocationMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.*;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    public List<EventFullDto> getEventsByAdmin(List<Long> users, List<EventState> states,
                                               List<Long> categories, LocalDateTime rangeStart,
                                               LocalDateTime rangeEnd, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        if (rangeStart == null) rangeStart = LocalDateTime.now().minusYears(100);
        if (rangeEnd == null) rangeEnd = LocalDateTime.now().plusYears(100);

        List<Event> events = eventRepository.findAllForAdmin(users, states, categories, rangeStart, rangeEnd, pageable);
        Map<Long, Long> viewsMap = getViewsForEvents(events);
        Map<Long, Long> confirmedMap = requestRepository.countConfirmedByEvents(events);

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventFullDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        Event event = getEventOrThrow(eventId);

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new EventDateValidationException("Дата события должна быть не раньше чем через час от текущего момента");
        }

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие должно быть в статусе PENDING для публикации");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new EventDateValidationException("Дата события должна быть не раньше чем через час от текущего момента");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (updateRequest.getStateAction() == AdminStateAction.REJECT_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие должно быть в статусе PENDING для отклонения");
                }
                event.setState(EventState.CANCELED);
                event.setPublishedOn(null);
            }
        }

        updateEventFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(), updateRequest.getRequestModeration(),
                updateRequest.getTitle());

        event = eventRepository.save(event);
        Long confirmedRequests = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
        Long views = getViewsForEvent(event.getId());
        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size) {
        User user = getUserOrThrow(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findByInitiator(user, pageable);
        Map<Long, Long> viewsMap = getViewsForEvents(events);
        Map<Long, Long> confirmedMap = requestRepository.countConfirmedByEvents(events);

        return events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventShortDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto newEventDto) {
        User user = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(newEventDto.getCategory());

        if (newEventDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new EventDateValidationException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }

        Location location = locationRepository.save(LocationMapper.toLocation(newEventDto.getLocation()));

        Event event = EventMapper.toEvent(newEventDto, category, user, location);
        event = eventRepository.save(event);

        return EventMapper.toEventFullDto(event, 0L, 0L);
    }

    @Override
    public EventFullDto getEventByUserAndId(Long userId, Long eventId) {
        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Доступ запрещён");
        }
        Long confirmedRequests = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
        Long views = getViewsForEvent(event.getId());
        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException("Доступ запрещён");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Только события в статусе PENDING или CANCELED могут быть изменены");
        }

        if (updateRequest.getEventDate() != null &&
                updateRequest.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new EventDateValidationException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }

        if (updateRequest.getStateAction() != null) {
            if (updateRequest.getStateAction() == UserStateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            } else if (updateRequest.getStateAction() == UserStateAction.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            }
        }

        updateEventFields(event, updateRequest.getAnnotation(), updateRequest.getCategory(),
                updateRequest.getDescription(), updateRequest.getEventDate(), updateRequest.getLocation(),
                updateRequest.getPaid(), updateRequest.getParticipantLimit(), updateRequest.getRequestModeration(),
                updateRequest.getTitle());

        event = eventRepository.save(event);
        Long confirmedRequests = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
        Long views = getViewsForEvent(event.getId());
        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    @Override
    public List<EventShortDto> getEventsPublic(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable,
                                               String sort, Integer from, Integer size, HttpServletRequest request) {

        saveHit(request);

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        if (rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("rangeStart must be before rangeEnd");
        }

        Pageable pageable = PageRequest.of(from / size, size);

        List<Event> events = eventRepository.findPublishedEventsWithFilters(
                text, categories, paid, rangeStart, rangeEnd, pageable);

        Map<Long, Long> viewsMap = getViewsForEvents(events);
        Map<Long, Long> confirmedMap = requestRepository.countConfirmedByEvents(events);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    Long confirmedRequests = confirmedMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventShortDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());

        if (onlyAvailable != null && onlyAvailable) {
            Map<Long, Long> finalConfirmedMap = confirmedMap;
            result = result.stream()
                    .filter(dto -> {
                        Long confirmed = finalConfirmedMap.getOrDefault(dto.getId(), 0L);
                        Event event = events.stream()
                                .filter(e -> e.getId().equals(dto.getId()))
                                .findFirst()
                                .orElse(null);
                        if (event == null) return false;
                        return event.getParticipantLimit() == 0 || confirmed < event.getParticipantLimit();
                    })
                    .collect(Collectors.toList());
        }

        if ("VIEWS".equals(sort)) {
            result.sort((a, b) -> Long.compare(b.getViews(), a.getViews()));
        } else if ("EVENT_DATE".equals(sort)) {
            result.sort(Comparator.comparing(EventShortDto::getEventDate));
        }

        return result;
    }

    @Override
    public EventFullDto getEventPublic(Long id, HttpServletRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Событие с id " + id + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не найдено");
        }

        Long views = getViewsForEvent(id);
        Long confirmedRequests = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);

        saveHit(request);

        return EventMapper.toEventFullDto(event, confirmedRequests, views + 1);
    }

    private void saveHit(HttpServletRequest request) {
        try {
            EndpointHit hit = EndpointHit.builder()
                    .app("ewm-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();
            statsClient.saveHit(hit);
        } catch (Exception e) {
            log.error("Failed to save hit: {}", e.getMessage());
        }
    }

    private Long getViewsForEvent(Long eventId) {
        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.now().plusYears(100);
        List<String> uris = List.of("/events/" + eventId);

        try {
            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);
            if (stats != null && !stats.isEmpty()) {
                return stats.get(0).getHits();
            }
        } catch (Exception e) {
            log.error("Failed to get views for event {}: {}", eventId, e.getMessage());
        }
        return 0L;
    }

    private Map<Long, Long> getViewsForEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return new HashMap<>();
        }

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        LocalDateTime end = LocalDateTime.now().plusYears(100);

        try {
            List<ViewStats> stats = statsClient.getStats(start, end, uris, true);
            Map<Long, Long> viewsMap = new HashMap<>();

            for (ViewStats stat : stats) {
                String uri = stat.getUri();
                if (uri != null && uri.startsWith("/events/")) {
                    try {
                        Long eventId = Long.parseLong(uri.substring(8));
                        viewsMap.put(eventId, stat.getHits());
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse event id from uri: {}", uri);
                    }
                }
            }
            return viewsMap;
        } catch (Exception e) {
            log.error("Failed to get views for events: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + categoryId + " не найдена"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private void updateEventFields(Event event, String annotation, Long categoryId, String description,
                                   LocalDateTime eventDate, LocationDto locationDto, Boolean paid,
                                   Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null) event.setAnnotation(annotation);
        if (categoryId != null) event.setCategory(getCategoryOrThrow(categoryId));
        if (description != null) event.setDescription(description);
        if (eventDate != null) event.setEventDate(eventDate);
        if (locationDto != null) {
            Location location = locationRepository.save(LocationMapper.toLocation(locationDto));
            event.setLocation(location);
        }
        if (paid != null) event.setPaid(paid);
        if (participantLimit != null) event.setParticipantLimit(participantLimit);
        if (requestModeration != null) event.setRequestModeration(requestModeration);
        if (title != null) event.setTitle(title);
    }
}