package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.CreateUserRequest;
import com.innowise.userservice.dto.UpdateUserRequest;
import com.innowise.userservice.dto.UserListResponseDto;
import com.innowise.userservice.dto.UserResponseDto;
import com.innowise.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        uses = PaymentCardMapper.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "paymentCards", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest request);

    UserResponseDto toDto(User user);

    UserListResponseDto toListDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "paymentCards", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);
}