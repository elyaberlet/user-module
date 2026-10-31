package com.innowise.userservice.service;

import com.innowise.userservice.dto.CreateUserRequest;
import com.innowise.userservice.dto.UpdateUserRequest;
import com.innowise.userservice.dto.UserListResponseDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.EmailAlreadyExistsException;
import com.innowise.userservice.exception.UserNotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Ivan");
        user.setSurname("Ivanov");
        user.setBirthDate(LocalDate.of(1995, 5, 10));
        user.setEmail("ivan@test.com");
        user.setActive(true);
        return user;
    }

    private UserResponseDto createUserResponse(Long id) {
        return new UserResponseDto(
                id,
                "Ivan",
                "Ivanov",
                LocalDate.of(1995, 5, 10),
                "ivan@test.com",
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of()
        );
    }

    private UserListResponseDto createUserListResponse(Long id) {
        return new UserListResponseDto(
                id,
                "Ivan",
                "Ivanov",
                LocalDate.of(1995, 5, 10),
                "ivan@test.com",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createUser_shouldReturnCreatedUser() {
        CreateUserRequest request = new CreateUserRequest(
                "Ivan",
                "Ivanov",
                LocalDate.of(1995, 5, 10),
                "ivan@test.com"
        );

        User user = createUser(1L);
        UserResponseDto response = createUserResponse(1L);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("ivan@test.com", result.email());

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
    }

    @Test
    void createUser_shouldThrowWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest(
                "Ivan",
                "Ivanov",
                null,
                "ivan@test.com"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(userMapper, never()).toEntity(any(CreateUserRequest.class));
    }

    @Test
    void getUserById_shouldReturnUserWithoutCards() {
        Long userId = 1L;
        User user = createUser(userId);
        UserListResponseDto response = createUserListResponse(userId);

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.of(user));
        when(userMapper.toListDto(user)).thenReturn(response);

        UserListResponseDto result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("Ivan", result.name());

        verify(userRepository).findByIdAndActiveTrue(userId);
        verify(userMapper).toListDto(user);
    }

    @Test
    void getUserById_shouldThrowWhenUserNotFound() {
        Long userId = 99L;

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(userId)
        );

        verify(userMapper, never()).toListDto(any(User.class));
    }

    @Test
    void getUsers_shouldReturnPageOfUsersWithoutCards() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = createUser(1L);
        UserListResponseDto response = createUserListResponse(1L);

        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<User>>any(),
                eq(pageable)
        )).thenReturn(userPage);

        when(userMapper.toListDto(user)).thenReturn(response);

        Page<UserListResponseDto> result =
                userService.getUsers("Ivan", "Ivanov", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Ivan", result.getContent().get(0).name());

        verify(userRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<User>>any(),
                eq(pageable)
        );
        verify(userMapper).toListDto(user);
    }

    @Test
    void getMyProfile_shouldReturnUserWithCards() {
        Long userId = 1L;
        User user = createUser(userId);
        UserResponseDto response = createUserResponse(userId);

        when(userRepository.findActiveWithCardsById(userId))
                .thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.getMyProfile(userId);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("Ivan", result.name());

        verify(userRepository).findActiveWithCardsById(userId);
        verify(userMapper).toDto(user);
    }

    @Test
    void getMyProfile_shouldThrowWhenUserNotFound() {
        Long userId = 99L;

        when(userRepository.findActiveWithCardsById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getMyProfile(userId)
        );

        verify(userMapper, never()).toDto(any(User.class));
    }

    @Test
    void updateUser_shouldUpdateAndReturnUser() {
        Long userId = 1L;

        UpdateUserRequest request = new UpdateUserRequest(
                "Petr",
                null,
                null,
                null
        );

        User user = createUser(userId);
        UserResponseDto response = new UserResponseDto(
                userId,
                "Petr",
                "Ivanov",
                user.getBirthDate(),
                user.getEmail(),
                true,
                null,
                null,
                List.of()
        );

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.updateUser(userId, request);

        assertEquals(userId, result.id());
        assertEquals("Petr", result.name());

        verify(userMapper).updateEntity(request, user);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_shouldThrowWhenUserNotFound() {
        Long userId = 99L;
        UpdateUserRequest request =
                new UpdateUserRequest("Petr", null, null, null);

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(userId, request)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(userMapper, never()).updateEntity(
                any(UpdateUserRequest.class),
                any(User.class)
        );
    }

    @Test
    void updateUser_shouldThrowWhenNewEmailAlreadyExists() {
        Long userId = 1L;

        UpdateUserRequest request = new UpdateUserRequest(
                null,
                null,
                null,
                "another@test.com"
        );

        User user = createUser(userId);

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("another@test.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.updateUser(userId, request)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(userMapper, never()).updateEntity(
                any(UpdateUserRequest.class),
                any(User.class)
        );
    }

    @Test
    void updateUser_shouldAllowExistingEmail() {
        Long userId = 1L;

        UpdateUserRequest request = new UpdateUserRequest(
                "Ivan",
                null,
                null,
                "ivan@test.com"
        );

        User user = createUser(userId);
        UserResponseDto response = createUserResponse(userId);

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(response);

        UserResponseDto result = userService.updateUser(userId, request);

        assertEquals("ivan@test.com", result.email());

        verify(userRepository, never()).existsByEmail(any(String.class));
        verify(userMapper).updateEntity(request, user);
    }

    @Test
    void deleteUser_shouldDeleteUser() {
        Long userId = 1L;
        User user = createUser(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> userService.deleteUser(userId));

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrowWhenUserNotFound() {
        Long userId = 99L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(userId)
        );

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void updateUserStatus_shouldUpdateStatus() {
        Long userId = 1L;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(userRepository.updateActiveStatus(userId, false)).thenReturn(1);

        assertDoesNotThrow(
                () -> userService.updateUserStatus(userId, false)
        );

        verify(userRepository).updateActiveStatus(userId, false);
    }

    @Test
    void updateUserStatus_shouldThrowWhenUserNotFound() {
        Long userId = 99L;

        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUserStatus(userId, false)
        );

        verify(userRepository, never())
                .updateActiveStatus(any(), anyBoolean());
    }

    @ParameterizedTest
    @ValueSource(longs = {10L, 20L, 30L})
    void getUserById_shouldUseRequestedId(Long userId) {
        User user = createUser(userId);
        UserListResponseDto response = createUserListResponse(userId);

        when(userRepository.findByIdAndActiveTrue(userId))
                .thenReturn(Optional.of(user));
        when(userMapper.toListDto(user)).thenReturn(response);

        UserListResponseDto result = userService.getUserById(userId);

        assertEquals(userId, result.id());
        verify(userRepository).findByIdAndActiveTrue(userId);
    }
}
