package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.UserDto;
import ru.practicum.ewm.dto.NewUserRequest;

import java.util.List;

public interface UserService {
    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    UserDto createUser(NewUserRequest newUserRequest);

    void deleteUser(Long userId);
}
