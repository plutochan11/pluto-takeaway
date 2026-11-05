package com.pluto.takeaway;

import com.pluto.takeaway.entity.Meal;
import com.pluto.takeaway.entity.MealOption;
import com.pluto.takeaway.entity.MealOptionGroup;
import com.pluto.takeaway.repository.MealRepository;
import com.pluto.takeaway.repository.MealOptionGroupRepository;
import com.pluto.takeaway.repository.MealOptionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import java.sql.SQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseConfiguration.class)
@Transactional
class MealSchemaTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MealRepository meals;

    @Autowired
    private MealOptionGroupRepository groups;

    @Autowired
    private MealOptionRepository options;

    @BeforeEach
    void createTwoShopMealStructures() {
        jdbc.update("""
            INSERT INTO meal (id, shop_id, category_id, name, base_price)
            VALUES (88001, 1, 10, 'Schema Meal A', 58.00),
                   (88002, 2, 20, 'Schema Meal B', 60.00)
            """);
        jdbc.update("""
            INSERT INTO meal_option_group (id, shop_id, meal_id, name)
            VALUES (88101, 1, 88001, 'Main A'),
                   (88102, 2, 88002, 'Drink B')
            """);
    }

    @Test
    void shouldAcceptProductAndNonProductOptionsWithSafeDefaults() {
        insertOption(1, 88101, 100L, null, "3.00");
        insertOption(1, 88101, null, "No Drink", "0.00");

        assertThat(optionCount()).isEqualTo(2);
        assertThat(jdbc.queryForObject(
            "SELECT status FROM meal WHERE id = 88001", String.class
        )).isEqualTo("UNAVAILABLE");
        assertThat(jdbc.queryForList(
            "SELECT status FROM meal_option WHERE group_id = 88101", String.class
        )).containsOnly("UNAVAILABLE");
        assertThat(jdbc.queryForList(
            "SELECT sort_order FROM meal_option WHERE group_id = 88101", Integer.class
        )).containsOnly(0);
    }

    @Test
    void shouldRejectMealInAnotherShopsCategory() {
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO meal (shop_id, category_id, name, base_price)
            VALUES (1, 20, 'Wrong category', 58.00)
            """))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("fk_meal_category");
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM meal WHERE name = 'Wrong category'", Long.class
        )).isZero();
    }

    @Test
    void shouldRejectGroupForAnotherShopsMeal() {
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO meal_option_group (shop_id, meal_id, name)
            VALUES (1, 88002, 'Wrong meal')
            """))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("fk_meal_group_meal");
    }

    @Test
    void shouldRejectOptionForAnotherShopsGroup() {
        assertThatThrownBy(() -> insertOption(1, 88102, 100L, null, "0.00"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("fk_meal_option_group");
        assertThat(optionCount()).isZero();
    }

    @Test
    void shouldRejectProductFromAnotherShop() {
        // Group and option are Shop 1; product 200 belongs to Shop 2.
        assertThatThrownBy(() -> insertOption(1, 88101, 200L, null, "3.00"))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("fk_meal_option_product");
        assertThat(optionCount()).isZero();
    }

    @Test
    void shouldRejectInvalidOptionShapes() {
        assertInvalidShape(100L, "Extra label", "0.00");
        assertInvalidShape(null, null, "0.00");
        assertInvalidShape(null, "   ", "0.00");
        assertInvalidShape(null, "No Drink", "1.00");
        assertThat(optionCount()).isZero();
    }

    @Test
    void shouldRejectNegativeSupplement() {
        assertThatThrownBy(() -> insertOption(1, 88101, 100L, null, "-1.00"))
            .isInstanceOf(DataAccessException.class)
            .hasCauseInstanceOf(SQLException.class)
            .satisfies(error -> assertThat(((SQLException) error.getCause()).getErrorCode())
                .isEqualTo(3819))
            .hasMessageContaining("chk_meal_option_supplement");
        assertThat(optionCount()).isZero();
    }

    @Test
    void shouldMapMealsAndFilterByShopInIdOrder() {
        jdbc.update("""
            INSERT INTO meal (id, shop_id, category_id, name, base_price, status)
            VALUES (88000, 1, 10, 'Earlier Meal A', 61.25, 'AVAILABLE')
            """);

        var result = meals.findByShopIdOrderByIdAsc(1L);
        assertThat(result).extracting(Meal::getId).containsExactly(88000L, 88001L);
        Meal meal = result.getFirst();
        assertThat(meal.getShopId()).isEqualTo(1L);
        assertThat(meal.getCategoryId()).isEqualTo(10L);
        assertThat(meal.getName()).isEqualTo("Earlier Meal A");
        assertThat(meal.getBasePrice()).isEqualByComparingTo("61.25");
        assertThat(meal.getStatus()).isEqualTo("AVAILABLE");
        assertThat(meals.findByShopIdOrderByIdAsc(2L))
            .extracting(Meal::getId).containsExactly(88002L);
    }

    @Test
    void shouldMapGroupsAndApplyParentScopeAndStableOrdering() {
        jdbc.update("UPDATE meal_option_group SET sort_order = 20 WHERE id = 88101");
        jdbc.update("""
            INSERT INTO meal_option_group (id, shop_id, meal_id, name, sort_order)
            VALUES (88104, 1, 88001, 'Second tie', 10),
                   (88103, 1, 88001, 'First tie', 10)
            """);

        var result = groups.findByShopIdAndMealIdOrderBySortOrderAscIdAsc(1L, 88001L);
        assertThat(result).extracting(MealOptionGroup::getId)
            .containsExactly(88103L, 88104L, 88101L);
        MealOptionGroup group = result.getFirst();
        assertThat(group.getShopId()).isEqualTo(1L);
        assertThat(group.getMealId()).isEqualTo(88001L);
        assertThat(group.getName()).isEqualTo("First tie");
        assertThat(group.getSortOrder()).isEqualTo(10);
        assertThat(groups.findByShopIdAndMealIdOrderBySortOrderAscIdAsc(2L, 88001L)).isEmpty();
        assertThat(groups.findByShopIdAndMealIdOrderBySortOrderAscIdAsc(1L, 88002L)).isEmpty();
    }

    @Test
    void shouldMapBothOptionShapesAndApplyParentScopeAndStableOrdering() {
        jdbc.update("""
            INSERT INTO meal_option
                (id, shop_id, group_id, product_id, label, supplement, status, sort_order)
            VALUES (88203, 1, 88101, NULL, 'No Drink', 0.00, 'AVAILABLE', 10),
                   (88202, 1, 88101, 100, NULL, 3.25, 'AVAILABLE', 10),
                   (88201, 1, 88101, 100, NULL, 5.00, 'UNAVAILABLE', 20),
                   (88204, 2, 88102, 200, NULL, 2.00, 'AVAILABLE', 0)
            """);

        var result = options.findByShopIdAndGroupIdOrderBySortOrderAscIdAsc(1L, 88101L);
        assertThat(result).extracting(MealOption::getId)
            .containsExactly(88202L, 88203L, 88201L);
        MealOption productOption = result.getFirst();
        assertThat(productOption.getShopId()).isEqualTo(1L);
        assertThat(productOption.getGroupId()).isEqualTo(88101L);
        assertThat(productOption.getProductId()).isEqualTo(100L);
        assertThat(productOption.getLabel()).isNull();
        assertThat(productOption.getSupplement()).isEqualByComparingTo("3.25");
        assertThat(productOption.getStatus()).isEqualTo("AVAILABLE");
        assertThat(productOption.getSortOrder()).isEqualTo(10);
        MealOption noDrink = result.get(1);
        assertThat(noDrink.getProductId()).isNull();
        assertThat(noDrink.getLabel()).isEqualTo("No Drink");
        assertThat(noDrink.getSupplement()).isEqualByComparingTo("0.00");
        assertThat(result.get(2).getStatus()).isEqualTo("UNAVAILABLE");
        assertThat(options.findByShopIdAndGroupIdOrderBySortOrderAscIdAsc(2L, 88101L)).isEmpty();
        assertThat(options.findByShopIdAndGroupIdOrderBySortOrderAscIdAsc(1L, 88102L)).isEmpty();
    }

    private void assertInvalidShape(Long productId, String label, String supplement) {
        assertThatThrownBy(() -> insertOption(1, 88101, productId, label, supplement))
            .isInstanceOf(DataAccessException.class)
            .hasCauseInstanceOf(SQLException.class)
            .satisfies(error -> assertThat(((SQLException) error.getCause()).getErrorCode())
                .isEqualTo(3819))
            .hasMessageContaining("chk_meal_option_shape");
    }

    private void insertOption(long shopId, long groupId, Long productId,
                              String label, String supplement) {
        jdbc.update("""
            INSERT INTO meal_option (shop_id, group_id, product_id, label, supplement)
            VALUES (?, ?, ?, ?, ?)
            """, shopId, groupId, productId, label, new java.math.BigDecimal(supplement));
    }

    private long optionCount() {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM meal_option WHERE group_id IN (88101, 88102)",
            Long.class
        );
    }
}
