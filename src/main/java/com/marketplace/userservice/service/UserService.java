package com.marketplace.userservice.service;

import com.marketplace.userservice.dto.CreateUserRequest;
import com.marketplace.userservice.dto.UpdateUserRequest;
import com.marketplace.userservice.dto.UserResponseDto;
import com.marketplace.userservice.entity.User;
import com.marketplace.userservice.exception.UserNotFoundException;
import com.marketplace.userservice.mapper.UserMapper;
import com.marketplace.userservice.repository.UserRepository;
import com.marketplace.userservice.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional

    public UserResponseDto createUser(CreateUserRequest requset) {
        User user = userMapper.toEntity(requset);
        User saved = userRepository.save(user);
        return userMapper.toResponseDto(saved);
    }

    public Page<UserResponseDto> getUsers(String name, String surname, Pageable pageable) {
        Specification<User> spec = UserSpecification.filterByNameAndSurname(name, surname);
        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(userMapper::toResponseDto);
    }

    @Cacheable(value = "users", key = "#userId")
    public UserResponseDto getUserById(Long userId) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return userMapper.toResponseDto(user);
    }

    @Transactional
    @CachePut(value = "users", key = "#id")
    public UserResponseDto updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        userMapper.updateEntity(request, user);
        User updated = userRepository.save(user);
        return userMapper.toResponseDto(updated);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long userId) {
        User user = userRepository.findByIdAndActiveTrue(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        userRepository.updateActiveStatus(userId, false);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void updateUserStatus(Long userId, Boolean active) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        userRepository.updateActiveStatus(userId, active);
    }
}
