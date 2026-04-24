package recipes.recipesfromdbservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import recipes.recipesfromdbservice.dtos.Languages;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "card_view", schema = "cookbook_wh")
public class RecipeCard {

    @Id
    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "lang")
    private String lang;

    @Column(name = "title")
    private String title;

    @Column(name = "image")
    private String image;

    @Column(name = "category")
    private String category;

    @Column(name = "ingredients_count")
    private Integer ingredientsCount;

    @Column(name = "instructions_count")
    private Integer instructionsCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ingredients")
    private JsonNode ingredients;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "instruction_steps")
    private JsonNode instructionSteps;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nutritions")
    private JsonNode nutritions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "times")
    private JsonNode times;

    @Column(name = "block_diet_keys")
    private List<String> blockDietKeys;

    @Column(name = "block_allergy_keys")
    private List<String> blockAllergyKeys;

    @Column(name = "block_health_keys")
    private List<String> blockHealthKeys;

    @Column(name = "caution_health_keys")
    private List<String> cautionHealthKeys;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "constraints")
    private JsonNode constraints;

}
