package recipes.householdservice.household.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import recipes.householdservice.dto.household.HouseholdMessageResponse;
import recipes.householdservice.household.model.HouseholdMessage;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HouseholdMessageMapper {

    HouseholdMessageResponse toResponse(HouseholdMessage entity);
}
