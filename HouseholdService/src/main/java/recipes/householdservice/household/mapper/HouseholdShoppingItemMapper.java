package recipes.householdservice.household.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import recipes.householdservice.dto.household.CreateHouseholdShoppingItemRequest;
import recipes.householdservice.dto.household.HouseholdShoppingItemResponse;
import recipes.householdservice.household.model.HouseholdShoppingItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HouseholdShoppingItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "householdId", ignore = true)
    @Mapping(target = "checked", ignore = true)
    @Mapping(target = "addedByUserId", ignore = true)
    @Mapping(target = "addedByName", ignore = true)
    @Mapping(target = "checkedByUserId", ignore = true)
    @Mapping(target = "checkedByName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    HouseholdShoppingItem toEntity(CreateHouseholdShoppingItemRequest request);

    HouseholdShoppingItemResponse toResponse(HouseholdShoppingItem entity);
}
