package com.pluto.takeaway;

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
