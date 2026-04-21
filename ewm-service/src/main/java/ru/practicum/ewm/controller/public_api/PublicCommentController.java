package ru.practicum.ewm.controller.public_api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CommentDto;
import ru.practicum.ewm.service.CommentService;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> getEventComments(@PathVariable Long eventId,
                                             @RequestParam(defaultValue = "0") Integer from,
                                             @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /events/{}/comments", eventId);
        return commentService.getEventComments(eventId, from, size);
    }
}
