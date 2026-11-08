package com.pluto.takeaway;

import com.pluto.takeaway.dto.CreateMealGroupRequest;
import com.pluto.takeaway.dto.CreateMealOptionRequest;
import com.pluto.takeaway.dto.CreateMealRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CreateMealRequestValidationTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void shouldAcceptProductAndNoDrinkChoices() {
        assertThat(paths(meal(List.of(group(List.of(
            option(100L, null, "3.00"),
            option(null, "No Drink", "0.00")
        )))))).isEmpty();
    }

    @Test
    void shouldRejectTheSameProductTwiceInOneGroup() {
        var request = meal(List.of(group(List.of(
            option(100L, null, "0.00"),
            option(100L, null, "3.00")
        ))));

        assertThat(paths(request)).containsExactly("groups[0].productSelectionUnique");
    }

    @Test
    void shouldAllowTheSameProductInDifferentGroups() {
        var request = meal(List.of(
            group(List.of(option(100L, null, "0.00"))),
            new CreateMealGroupRequest("Side", 10,
                List.of(option(100L, null, "3.00")))
        ));

        assertThat(paths(request)).isEmpty();
    }

    @Test
    void shouldRejectInvalidOptionCombinations() {
        var invalidOptions = List.of(
            option(100L, "Coffee", "3.00"),
            option(null, null, "0.00"),
            option(null, "", "0.00"),
            option(null, "   ", "0.00"),
            option(null, "No Drink", "1.00")
        );
        for (var invalid : invalidOptions) {
            assertThat(paths(invalid)).as("option %s", invalid)
                .contains("validOptionConfiguration");
        }
    }

    @Test
    void shouldRejectOutOfRangeAndOverpreciseMoney() {
        for (String value : List.of("-0.01", "10000.00", "1.001")) {
            var request = new CreateMealRequest(10L, "Lunch", new BigDecimal(value),
                List.of(group(List.of(option(100L, null, "0.00")))));
            assertThat(paths(request)).as("basePrice %s", value).contains("basePrice");
            assertThat(paths(option(100L, null, value)))
                .as("supplement %s", value).contains("supplement");
        }
        assertThat(paths(option(100L, null, null))).contains("supplement");
        var missingPrice = new CreateMealRequest(10L, "Lunch", null,
            List.of(group(List.of(option(100L, null, "0.00")))));
        assertThat(paths(missingPrice)).contains("basePrice");
    }

    @Test
    void shouldAcceptZeroAndMaximumMonetaryInputs() {
        for (String value : List.of("0.00", "9999.99")) {
            var request = new CreateMealRequest(10L, "Lunch", new BigDecimal(value),
                List.of(group(List.of(option(100L, null, value)))));
            assertThat(paths(request)).as("money %s", value).isEmpty();
        }
    }

    @Test
    void shouldRejectMissingEmptyOrNullElementGroups() {
        assertThat(paths(meal(null))).contains("groups");
        assertThat(paths(meal(List.of()))).contains("groups");
        assertThat(paths(meal(Arrays.asList((CreateMealGroupRequest) null))))
            .anyMatch(path -> path.startsWith("groups[0]"));
    }

    @Test
    void shouldRejectMissingEmptyOrNullElementOptions() {
        assertThat(paths(meal(List.of(group(null)))))
            .contains("groups[0].options");
        assertThat(paths(meal(List.of(group(List.of())))))
            .contains("groups[0].options");
        assertThat(paths(meal(List.of(group(
            Arrays.asList((CreateMealOptionRequest) null))))))
            .anyMatch(path -> path.startsWith("groups[0].options[0]"));
    }

    @Test
    void shouldCascadeToGroupConstraints() {
        var invalidGroup = new CreateMealGroupRequest(" ", -1,
            List.of(option(100L, null, "0.00")));
        assertThat(paths(meal(List.of(invalidGroup))))
            .contains("groups[0].name", "groups[0].sortOrder");
    }

    @Test
    void shouldCascadeToOptionFieldConstraints() {
        var invalidOption = new CreateMealOptionRequest(0L, null,
            new BigDecimal("1.001"), -1);
        assertThat(paths(meal(List.of(group(List.of(invalidOption))))))
            .contains("groups[0].options[0].productId",
                "groups[0].options[0].supplement", "groups[0].options[0].sortOrder");
    }

    @Test
    void shouldCascadeToComputedOptionConstraint() {
        var invalidOption = option(null, "No Drink", "3.00");
        assertThat(paths(meal(List.of(group(List.of(invalidOption))))))
            .contains("groups[0].options[0].validOptionConfiguration");
    }

    @Test
    void shouldIgnoreComputedPropertyInJsonButStillValidateIt() throws Exception {
        JsonMapper mapper = JsonMapper.builder().build();
        var invalidOption = option(null, "No Drink", "3.00");
        String json = mapper.writeValueAsString(invalidOption);
        var tree = mapper.readTree(json);
        assertThat(tree.has("validOptionConfiguration")).isFalse();
        assertThat(tree.has("label")).isTrue();
        var decoded = mapper.readValue(json, CreateMealOptionRequest.class);
        assertThat(paths(decoded)).contains("validOptionConfiguration");
    }

    private static Set<String> paths(Object request) {
        return validator.validate(request).stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }

    private static CreateMealRequest meal(List<CreateMealGroupRequest> groups) {
        return new CreateMealRequest(10L, "Lunch", new BigDecimal("58.00"), groups);
    }

    private static CreateMealGroupRequest group(List<CreateMealOptionRequest> options) {
        return new CreateMealGroupRequest("Drink", 0, options);
    }

    private static CreateMealOptionRequest option(Long productId, String label, String amount) {
        return new CreateMealOptionRequest(productId, label,
            amount == null ? null : new BigDecimal(amount), 0);
    }
}
