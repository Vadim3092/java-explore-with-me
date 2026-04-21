package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;
import ru.practicum.ewm.error.ConflictException;
import ru.practicum.ewm.error.ForbiddenException;
import ru.practicum.ewm.error.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<CommentDto> getEventComments(Long eventId, Integer from, Integer size) {
        Event event = getEventOrThrow(eventId);
        Pageable pageable = PageRequest.of(from / size, size);
        return commentRepository.findByEventAndStatus(event, CommentStatus.PUBLISHED, pageable)
                .stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto dto) {
        User author = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = CommentMapper.toComment(dto, event, author);
        comment = commentRepository.save(comment);
        log.info("User {} created comment {} for event {}", userId, comment.getId(), eventId);
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto) {
        getUserOrThrow(userId);
        Comment comment = getCommentOrThrow(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Можно редактировать только свои комментарии");
        }

        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new ConflictException("Нельзя редактировать удалённый комментарий");
        }

        comment.setText(dto.getText());
        comment.setUpdatedOn(LocalDateTime.now());
        comment = commentRepository.save(comment);
        log.info("User {} updated comment {}", userId, commentId);
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    @Transactional
    public void deleteOwnComment(Long userId, Long commentId) {
        getUserOrThrow(userId);
        Comment comment = getCommentOrThrow(commentId);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ForbiddenException("Можно удалять только свои комментарии");
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
        log.info("User {} soft-deleted comment {}", userId, commentId);
    }

    @Override
    public List<CommentDto> getAllCommentsForAdmin(Long eventId, Long authorId, String status,
                                                   LocalDateTime start, LocalDateTime end,
                                                   Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        CommentStatus commentStatus = status != null ? CommentStatus.valueOf(status) : null;

        Page<Comment> commentsPage;

        if (start != null && end != null) {
            commentsPage = commentRepository.findAllForAdminWithDates(eventId, authorId, commentStatus, start, end, pageable);
        } else if (start != null) {
            commentsPage = commentRepository.findAllForAdminWithStart(eventId, authorId, commentStatus, start, pageable);
        } else if (end != null) {
            commentsPage = commentRepository.findAllForAdminWithEnd(eventId, authorId, commentStatus, end, pageable);
        } else {
            commentsPage = commentRepository.findAllForAdminNoDates(eventId, authorId, commentStatus, pageable);
        }

        return commentsPage.stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void hideCommentByAdmin(Long commentId) {
        Comment comment = getCommentOrThrow(commentId);
        comment.setStatus(CommentStatus.HIDDEN);
        commentRepository.save(comment);
        log.info("Admin hid comment {}", commentId);
    }

    @Override
    @Transactional
    public void unhideCommentByAdmin(Long commentId) {
        Comment comment = getCommentOrThrow(commentId);
        comment.setStatus(CommentStatus.PUBLISHED);
        commentRepository.save(comment);
        log.info("Admin unhid comment {}", commentId);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = getCommentOrThrow(commentId);
        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
        log.info("Admin soft-deleted comment {}", commentId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id " + eventId + " not found"));
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment with id " + commentId + " not found"));
    }
}
