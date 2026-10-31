package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.CardResponseDto;
import com.innowise.userservice.dto.CreateCardRequest;
import com.innowise.userservice.dto.UpdateCardRequest;
import com.innowise.userservice.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PaymentCardMapper {
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(CreateCardRequest request);

    CardResponseDto toDto(PaymentCard card);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateCardRequest request, @MappingTarget PaymentCard card);
}
