package ru.practicum.stats.server.mapper;

import ru.practicum.stats.dto.EndpointHit;
import ru.practicum.stats.server.model.EndpointHitEntity;

public class StatsMapper {

    public static EndpointHitEntity toEntity(EndpointHit hit) {
        if (hit == null) return null;
        return EndpointHitEntity.builder()
                .id(hit.getId())
                .app(hit.getApp())
                .uri(hit.getUri())
                .ip(hit.getIp())
                .timestamp(hit.getTimestamp())
                .build();
    }

    public static EndpointHit toDto(EndpointHitEntity entity) {
        if (entity == null) return null;
        return EndpointHit.builder()
                .id(entity.getId())
                .app(entity.getApp())
                .uri(entity.getUri())
                .ip(entity.getIp())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
