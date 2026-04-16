package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.ParticipationRequest;
import ru.practicum.ewm.model.RequestStatus;
import ru.practicum.ewm.model.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findByRequester(User requester);

    List<ParticipationRequest> findByEvent(Event event);

    List<ParticipationRequest> findByEventAndStatus(Event event, RequestStatus status);

    Optional<ParticipationRequest> findByRequesterAndEvent(User requester, Event event);

    Long countByEventAndStatus(Event event, RequestStatus status);

    @Query("""
            SELECT r.event.id as eventId, COUNT(r) as count
            FROM ParticipationRequest r
            WHERE r.event IN :events AND r.status = 'CONFIRMED'
            GROUP BY r.event.id
            """)
    List<Object[]> countConfirmedByEventsRaw(@Param("events") List<Event> events);

    default Map<Long, Long> countConfirmedByEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }
        return countConfirmedByEventsRaw(events).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
