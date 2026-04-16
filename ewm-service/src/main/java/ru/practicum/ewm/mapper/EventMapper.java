package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.EventState;
import java.time.LocalDateTime;

public class EventMapper {

    public static EventShortDto toEventShortDto(Event event, Long confirmedRequests, Long views) {
        if (event == null) return null;
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static EventFullDto toEventFullDto(Event event, Long confirmedRequests, Long views) {
        if (event == null) return null;
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .location(LocationMapper.toLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static Event toEvent(NewEventDto newEventDto, ru.practicum.ewm.model.Category category,
                                ru.practicum.ewm.model.User initiator, ru.practicum.ewm.model.Location location) {
        if (newEventDto == null) return null;
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .initiator(initiator)
                .location(location)
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ? newEventDto.getRequestModeration() : true)
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .title(newEventDto.getTitle())
                .build();
    }
}
