package ru.practicum.ewm.controller.public_api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.service.CategoryService;
import ru.practicum.ewm.service.CompilationService;
import ru.practicum.ewm.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(produces = "application/json")
@RequiredArgsConstructor
public class PublicController {

    private final CategoryService categoryService;
    private final CompilationService compilationService;
    private final EventService eventService;

    @GetMapping("/categories")
    public List<CategoryDto> getCategories(@RequestParam(defaultValue = "0") Integer from,
                                           @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /categories: from={}, size={}", from, size);
        return categoryService.getCategories(from, size);
    }

    @GetMapping("/categories/{catId}")
    public CategoryDto getCategoryById(@PathVariable Long catId) {
        log.info("GET /categories/{}", catId);
        return categoryService.getCategoryById(catId);
    }

    @GetMapping("/compilations")
    public List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                                @RequestParam(defaultValue = "0") Integer from,
                                                @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /compilations: pinned={}, from={}, size={}", pinned, from, size);
        return compilationService.getCompilations(pinned, from, size);
    }

    @GetMapping("/compilations/{compId}")
    public CompilationDto getCompilationById(@PathVariable Long compId) {
        log.info("GET /compilations/{}", compId);
        return compilationService.getCompilationById(compId);
    }

    @GetMapping("/events")
    public List<EventShortDto> getEvents(@RequestParam(required = false) String text,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                         @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                         @RequestParam(required = false) String sort,
                                         @RequestParam(defaultValue = "0") Integer from,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         HttpServletRequest request) {
        log.info("GET /events: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        return eventService.getEventsPublic(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, request);
    }

    @GetMapping("/events/{id}")
    public EventFullDto getEventById(@PathVariable Long id,
                                     HttpServletRequest request) {
        log.info("GET /events/{}", id);
        return eventService.getEventPublic(id, request);
    }
}
