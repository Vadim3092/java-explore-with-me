package ru.practicum.ewm.mapper;

import ru.practicum.ewm.dto.LocationDto;
import ru.practicum.ewm.model.Location;

public class LocationMapper {

    public static LocationDto toLocationDto(Location location) {
        if (location == null) return null;
        return LocationDto.builder()
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }

    public static Location toLocation(LocationDto locationDto) {
        if (locationDto == null) return null;
        return Location.builder()
                .lat(locationDto.getLat())
                .lon(locationDto.getLon())
                .build();
    }
}
