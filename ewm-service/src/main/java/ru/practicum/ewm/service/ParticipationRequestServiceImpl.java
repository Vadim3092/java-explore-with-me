package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.dto.ParticipationRequestDto;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.mapper.ParticipationRequestMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        User user = getUserOrThrow(userId);
        return requestRepository.findByRequester(user).stream()
                .map(ParticipationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User requester = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может подать заявку на участие в своём событии");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.findByRequesterAndEvent(requester, event).isPresent()) {
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        Long confirmedRequests = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        RequestStatus status;
        if (event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        } else if (!event.getRequestModeration()) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(status)
                .build();

        request = requestRepository.save(request);
        return ParticipationRequestMapper.toParticipationRequestDto(request);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        getUserOrThrow(userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Заявка с id " + requestId + " не найдена"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new RuntimeException("Доступ запрещён");
        }

        request.setStatus(RequestStatus.CANCELED);
        request = requestRepository.save(request);
        return ParticipationRequestMapper.toParticipationRequestDto(request);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new RuntimeException("Доступ запрещён");
        }

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new ConflictException("Модерация заявок отключена");
        }

        Long currentConfirmed = requestRepository.countByEventAndStatus(event, RequestStatus.CONFIRMED);
        if (currentConfirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequest.getRequestIds());

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();
        Long confirmedCount = currentConfirmed;

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок в состоянии ожидания");
            }

            if (updateRequest.getStatus() == RequestStatus.CONFIRMED) {
                if (confirmedCount < event.getParticipantLimit()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(request);
                    confirmedCount++;
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(request);
                }
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(request);
            }
        }

        requestRepository.saveAll(confirmed);
        requestRepository.saveAll(rejected);

        if (updateRequest.getStatus() == RequestStatus.CONFIRMED && confirmedCount >= event.getParticipantLimit()) {
            List<ParticipationRequest> pendingRequests = requestRepository.findByEventAndStatus(event, RequestStatus.PENDING);
            for (ParticipationRequest pr : pendingRequests) {
                if (!rejected.contains(pr) && !confirmed.contains(pr)) {
                    pr.setStatus(RequestStatus.REJECTED);
                    rejected.add(pr);
                }
            }
            requestRepository.saveAll(pendingRequests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed.stream()
                        .map(ParticipationRequestMapper::toParticipationRequestDto)
                        .collect(Collectors.toList()))
                .rejectedRequests(rejected.stream()
                        .map(ParticipationRequestMapper::toParticipationRequestDto)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public List<ParticipationRequestDto> getEventRequestsByUser(Long userId, Long eventId) {
        getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new RuntimeException("Доступ запрещён");
        }

        return requestRepository.findByEvent(event).stream()
                .map(ParticipationRequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь с id " + userId + " не найден"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Событие с id " + eventId + " не найдено"));
    }
}