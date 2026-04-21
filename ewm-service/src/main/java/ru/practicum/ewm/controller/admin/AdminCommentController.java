package ru.practicum.ewm.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.service.CommentService;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getAllComments(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /admin/comments");
        return commentService.getAllCommentsForAdmin(eventId, authorId, status, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/{commentId}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hideComment(@PathVariable Long commentId) {
        log.info("PATCH /admin/comments/{}/hide", commentId);
        commentService.hideCommentByAdmin(commentId);
    }

    @PatchMapping("/{commentId}/unhide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unhideComment(@PathVariable Long commentId) {
        log.info("PATCH /admin/comments/{}/unhide", commentId);
        commentService.unhideCommentByAdmin(commentId);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        log.info("DELETE /admin/comments/{}", commentId);
        commentService.deleteCommentByAdmin(commentId);
    }
}
