package com.userservice.pantry.mapper;

import com.userservice.pantry.dto.CreatePantryItemRequest;
import com.userservice.pantry.dto.PantryItemResponse;
import com.userservice.pantry.dto.PantryListItemResponse;
import com.userservice.pantry.dto.UpdatePantryItemRequest;
import com.userservice.pantry.model.PantryItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PantryItemMapper {

    PantryItemResponse toResponse(PantryItem entity);

    PantryListItemResponse toListItemResponse(PantryItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PantryItem toEntity(CreatePantryItemRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(UpdatePantryItemRequest request, @MappingTarget PantryItem entity);
}
