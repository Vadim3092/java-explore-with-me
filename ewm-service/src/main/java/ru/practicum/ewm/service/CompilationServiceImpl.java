package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.NewCompilationDto;
import ru.practicum.ewm.dto.UpdateCompilationRequest;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findByPinned(pinned, pageable).getContent();
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        if (compilations.isEmpty()) {
            return List.of();
        }

        List<Event> allEvents = compilations.stream()
                .flatMap(c -> c.getEvents().stream())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Long> confirmedMap = requestRepository.countConfirmedByEvents(allEvents);
        Map<Long, Long> viewsMap = getViewsForEvents(allEvents);

        return compilations.stream()
                .map(compilation -> toCompilationDto(compilation, confirmedMap, viewsMap))
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new RuntimeException("Подборка с id " + compId + " не найдена"));

        List<Event> events = compilation.getEvents();
        Map<Long, Long> confirmedMap = requestRepository.countConfirmedByEvents(events);
        Map<Long, Long> viewsMap = getViewsForEvents(events);

        return toCompilationDto(compilation, confirmedMap, viewsMap);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Creating compilation: {}", newCompilationDto);

        Compilation compilation = CompilationMapper.toCompilation(newCompilationDto);

        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(newCompilationDto.getEvents());
            compilation.setEvents(events);
        }

        compilation = compilationRepository.save(compilation);

        List<Event> events = compilation.getEvents();
        Map<Long, Long> confirmedMap = requestRepository.countConfirmedByEvents(events);
        Map<Long, Long> viewsMap = getViewsForEvents(events);

        return toCompilationDto(compilation, confirmedMap, viewsMap);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        log.info("Updating compilation with id: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new RuntimeException("Подборка с id " + compId + " не найдена"));

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(updateRequest.getEvents());
            compilation.setEvents(events);
        }

        compilation = compilationRepository.save(compilation);

        List<Event> events = compilation.getEvents();
        Map<Long, Long> confirmedMap = requestRepository.countConfirmedByEvents(events);
        Map<Long, Long> viewsMap = getViewsForEvents(events);

        return toCompilationDto(compilation, confirmedMap, viewsMap);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с id: {}", compId);
        compilationRepository.deleteById(compId);
    }

    private CompilationDto toCompilationDto(Compilation compilation,
                                            Map<Long, Long> confirmedMap,
                                            Map<Long, Long> viewsMap) {
        List<EventShortDto> eventShortDtos = compilation.getEvents().stream()
                .map(event -> {
                    Long confirmedRequests = confirmedMap.getOrDefault(event.getId(), 0L);
                    Long views = viewsMap.getOrDefault(event.getId(), 0L);
                    return EventMapper.toEventShortDto(event, confirmedRequests, views);
                })
                .collect(Collectors.toList());

        return CompilationMapper.toCompilationDto(compilation, eventShortDtos);
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
            List<ViewStats> stats = statsClient.getStats(start, end, uris, false);
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
}