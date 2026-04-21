package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.dto.NewCommentDto;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentService {

    List<CommentDto> getEventComments(Long eventId, Integer from, Integer size);

    CommentDto createComment(Long userId, Long eventId, NewCommentDto dto);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto dto);

    void deleteOwnComment(Long userId, Long commentId);

    List<CommentDto> getAllCommentsForAdmin(Long eventId, Long authorId, String status,
                                            LocalDateTime start, LocalDateTime end,
                                            Integer from, Integer size);

    void hideCommentByAdmin(Long commentId);

    void unhideCommentByAdmin(Long commentId);

    void deleteCommentByAdmin(Long commentId);
}
