package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.model.Compilation;
import java.util.List;

public class CompilationMapper {

    public static CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> eventShortDtos) {
        if (compilation == null) return null;
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(eventShortDtos)
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }

    public static Compilation toCompilation(ru.practicum.ewm.dto.NewCompilationDto newCompilationDto) {
        if (newCompilationDto == null) return null;
        return Compilation.builder()
                .pinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)
                .title(newCompilationDto.getTitle())
                .build();
    }
}
