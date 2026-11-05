package com.pluto.takeaway.repository;

import com.pluto.takeaway.entity.MealOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealOptionGroupRepository extends JpaRepository<MealOptionGroup, Long> {

	List<MealOptionGroup> findByShopIdAndMealIdOrderBySortOrderAscIdAsc(Long shopId, Long mealId);
}
