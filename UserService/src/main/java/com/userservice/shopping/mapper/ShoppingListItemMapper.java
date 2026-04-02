package com.userservice.shopping.mapper;

import com.userservice.shopping.dto.CreateShoppingListItemRequest;
import com.userservice.shopping.dto.ShoppingListItemResponse;
import com.userservice.shopping.model.ShoppingListItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ShoppingListItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "checked", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ShoppingListItem toEntity(CreateShoppingListItemRequest request);

    ShoppingListItemResponse toResponse(ShoppingListItem entity);
}
