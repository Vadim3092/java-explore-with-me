package ru.practicum.ewm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.CommentStatus;
import ru.practicum.ewm.model.Event;

import java.time.LocalDateTime;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByEventAndStatus(Event event, CommentStatus status, Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            WHERE (:eventId IS NULL OR c.event.id = :eventId)
              AND (:authorId IS NULL OR c.author.id = :authorId)
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<Comment> findAllForAdminNoDates(@Param("eventId") Long eventId,
                                         @Param("authorId") Long authorId,
                                         @Param("status") CommentStatus status,
                                         Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            WHERE (:eventId IS NULL OR c.event.id = :eventId)
              AND (:authorId IS NULL OR c.author.id = :authorId)
              AND (:status IS NULL OR c.status = :status)
              AND (c.createdOn >= :start)
            """)
    Page<Comment> findAllForAdminWithStart(@Param("eventId") Long eventId,
                                           @Param("authorId") Long authorId,
                                           @Param("status") CommentStatus status,
                                           @Param("start") LocalDateTime start,
                                           Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            WHERE (:eventId IS NULL OR c.event.id = :eventId)
              AND (:authorId IS NULL OR c.author.id = :authorId)
              AND (:status IS NULL OR c.status = :status)
              AND (c.createdOn <= :end)
            """)
    Page<Comment> findAllForAdminWithEnd(@Param("eventId") Long eventId,
                                         @Param("authorId") Long authorId,
                                         @Param("status") CommentStatus status,
                                         @Param("end") LocalDateTime end,
                                         Pageable pageable);

    @Query("""
            SELECT c FROM Comment c
            WHERE (:eventId IS NULL OR c.event.id = :eventId)
              AND (:authorId IS NULL OR c.author.id = :authorId)
              AND (:status IS NULL OR c.status = :status)
              AND (c.createdOn >= :start)
              AND (c.createdOn <= :end)
            """)
    Page<Comment> findAllForAdminWithDates(@Param("eventId") Long eventId,
                                           @Param("authorId") Long authorId,
                                           @Param("status") CommentStatus status,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           Pageable pageable);
}
