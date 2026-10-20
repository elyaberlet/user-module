package com.marketplace.userservice;

import com.marketplace.userservice.dto.CreateUserRequest;
import com.marketplace.userservice.dto.UpdateUserRequest;
import com.marketplace.userservice.dto.UserResponseDto;
import com.marketplace.userservice.entity.User;
import com.marketplace.userservice.exception.UserNotFoundException;
import com.marketplace.userservice.mapper.UserMapper;
import com.marketplace.userservice.repository.UserRepository;
import com.marketplace.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUserAndReturnDto() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("Иван")
                .surname("Иванов")
                .email("ivan@test.com")
                .build();

        User mockUser = new User();
        mockUser.setId(1L);
        UserResponseDto expectedDto = new UserResponseDto();
        expectedDto.setId(1L);

        when(userMapper.toEntity(request)).thenReturn(mockUser);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toResponseDto(mockUser)).thenReturn(expectedDto);

        UserResponseDto result = userService.createUser(request);

        assertEquals(expectedDto, result);
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void shouldReturnUserByIdWhenUserExists() {
        Long userId = 1L;
        User mockUser = new User();
        mockUser.setId(userId);
        UserResponseDto expectedDto = new UserResponseDto();
        expectedDto.setId(userId);

        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(mockUser));
        when(userMapper.toResponseDto(mockUser)).thenReturn(expectedDto);

        UserResponseDto result = userService.getUserById(userId);

        assertEquals(expectedDto, result);
        verify(userRepository, times(1)).findByIdAndActiveTrue(userId);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundById() {
        Long userId = 999L;
        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
    }

    @Test
    void shouldReturnPageOfUsersWhenFiltersApplied() {
        String name = "Иван";
        String surname = "Иванов";
        Pageable pageable = PageRequest.of(0, 10);

        User mockUser = new User();
        mockUser.setId(1L);
        Page<User> userPage = new PageImpl<>(List.of(mockUser), pageable, 1);
        UserResponseDto dto = new UserResponseDto();
        dto.setId(1L);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toResponseDto(mockUser)).thenReturn(dto);

        Page<UserResponseDto> result = userService.getUsers(name, surname, pageable);

        assertEquals(1, result.getTotalElements());
        verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldUpdateUserAndReturnUpdatedDto() {
        Long userId = 1L;
        UpdateUserRequest request = UpdateUserRequest.builder().name("Петр").build();

        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setName("Иван");
        UserResponseDto expectedDto = new UserResponseDto();
        expectedDto.setId(userId);
        expectedDto.setName("Петр");

        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(mockUser));
        doNothing().when(userMapper).updateEntity(request, mockUser);
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        when(userMapper.toResponseDto(mockUser)).thenReturn(expectedDto);

        UserResponseDto result = userService.updateUser(userId, request);

        assertEquals(expectedDto, result);
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        Long userId = 999L;
        UpdateUserRequest request = UpdateUserRequest.builder().build();
        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(userId, request));
    }

    @Test
    void shouldDeactivateUserWhenDeleted() {
        Long userId = 1L;
        User mockUser = new User();
        mockUser.setId(userId);

        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(mockUser));
        doNothing().when(userRepository).updateActiveStatus(userId, false);

        userService.deleteUser(userId);

        verify(userRepository, times(1)).updateActiveStatus(userId, false);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistentUser() {
        Long userId = 999L;
        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(userId));
    }

    @Test
    void shouldUpdateUserStatusSuccessfully() {
        Long userId = 1L;
        Boolean active = false;
        User mockUser = new User();
        mockUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        doNothing().when(userRepository).updateActiveStatus(userId, active);

        userService.updateUserStatus(userId, active);

        verify(userRepository, times(1)).updateActiveStatus(userId, active);
    }

    @ParameterizedTest
    @CsvSource({
            "1, Иван, Иванов",
            "2, Петр, Петров",
            "3, Анна, Сидорова"
    })
    void shouldReturnCorrectUserForDifferentIds(Long userId, String name, String surname) {
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setName(name);
        mockUser.setSurname(surname);

        UserResponseDto expectedDto = new UserResponseDto();
        expectedDto.setId(userId);
        expectedDto.setName(name);

        when(userRepository.findByIdAndActiveTrue(userId)).thenReturn(Optional.of(mockUser));
        when(userMapper.toResponseDto(mockUser)).thenReturn(expectedDto);

        UserResponseDto result = userService.getUserById(userId);

        assertEquals(expectedDto, result);
        assertEquals(name, result.getName());
    }
}