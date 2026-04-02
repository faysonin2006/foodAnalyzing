package recipes.householdservice.household.mapper;

import org.springframework.stereotype.Component;
import recipes.householdservice.dto.household.HouseholdMemberResponse;
import recipes.householdservice.dto.household.HouseholdResponse;
import recipes.householdservice.dto.household.HouseholdSummaryResponse;
import recipes.householdservice.household.model.Household;

import java.util.List;

@Component
public class HouseholdMapper {

    public HouseholdSummaryResponse toSummary(Household entity, long membersCount, long uncheckedItemsCount) {
        return HouseholdSummaryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .membersCount(membersCount)
                .uncheckedItemsCount(uncheckedItemsCount)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public HouseholdResponse toResponse(Household entity, List<HouseholdMemberResponse> members, long uncheckedItemsCount) {
        return HouseholdResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .createdByUserId(entity.getCreatedByUserId())
                .createdByEmail(entity.getCreatedByEmail())
                .uncheckedItemsCount(uncheckedItemsCount)
                .members(members)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
