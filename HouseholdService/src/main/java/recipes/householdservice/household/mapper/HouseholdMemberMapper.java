package recipes.householdservice.household.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import recipes.householdservice.dto.household.HouseholdMemberResponse;
import recipes.householdservice.household.model.HouseholdMember;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HouseholdMemberMapper {

    HouseholdMemberResponse toResponse(HouseholdMember entity);
}
