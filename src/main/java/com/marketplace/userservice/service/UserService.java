package com.marketplace.userservice.service;

import com.marketplace.userservice.dto.*;
import com.marketplace.userservice.entity.User;
import com.marketplace.userservice.exception.EmailAlreadyExistsException;
import com.marketplace.userservice.exception.UserNotFoundException;
import com.marketplace.userservice.mapper.UserMapper;
import com.marketplace.userservice.repository.UserRepository;
import com.marketplace.userservice.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public UserResponseDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) throw new EmailAlreadyExistsException(request.email());
        User user = userMapper.toEntity(request);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsers(String name, String surname, Pageable pageable) {
        Specification<User> spec = Specification.where(UserSpecification.activeOnly())
                .and(UserSpecification.hasName(name))
                .and(UserSpecification.hasSurname(surname));
        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "users", key = "#userId")
    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findActiveWithCardsById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return userMapper.toDto(user);
    }

    @Transactional
    @CachePut(cacheNames = "users", key = "#userId")
    public UserResponseDto updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        userMapper.updateEntity(request, user);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @CacheEvict(cacheNames = "users", allEntries = true),
            @CacheEvict(cacheNames = "cards", allEntries = true)
    })
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.delete(user);
    }

    @Transactional
    @CacheEvict(cacheNames = "users", key = "#userId")
    public void updateUserStatus(Long userId, boolean active) {
        if (!userRepository.existsById(userId)) throw new UserNotFoundException(userId);
        userRepository.updateActiveStatus(userId, active);
    }
}
