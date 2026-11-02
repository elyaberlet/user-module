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
import com.innowise.userservice.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "users", allEntries = true),
            @CacheEvict(cacheNames = "userProfiles", allEntries = true)
    })
    public UserResponseDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("User creation failed: email is already registered");
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        log.info("User created successfully: userId={}", savedUser.getId());

        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserListResponseDto> getUsers(
            String name,
            String surname,
            Pageable pageable) {

        Specification<User> spec = Specification.where(UserSpecification.activeOnly())
                .and(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname));

        return userRepository.findAll(spec, pageable)
                .map(userMapper::toListDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "users", key = "#userId")
    public UserListResponseDto getUserById(Long userId) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return userMapper.toListDto(user);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userProfiles", key = "#userId")
    public UserResponseDto getMyProfile(Long userId) {
        User user = userRepository.findActiveWithCardsById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return userMapper.toDto(user);
    }

    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "userProfiles", key = "#userId"),
            evict = @CacheEvict(cacheNames = "users", key = "#userId")
    )
    public UserResponseDto updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            log.warn("User update failed: email is already registered, userId={}", userId);
            throw new EmailAlreadyExistsException(request.email());
        }

        userMapper.updateEntity(request, user);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "users", allEntries = true),
            @CacheEvict(cacheNames = "userProfiles", allEntries = true),
            @CacheEvict(cacheNames = "cards", allEntries = true)
    })
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        userRepository.delete(user);

        log.info("User deleted successfully: userId={}", userId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "users", key = "#userId"),
            @CacheEvict(cacheNames = "userProfiles", key = "#userId")
    })
    public void updateUserStatus(Long userId, boolean active) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        userRepository.updateActiveStatus(userId, active);
    }
}
