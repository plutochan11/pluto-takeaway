package com.pluto.takeaway.repository;

import com.pluto.takeaway.entity.Meal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {

	List<Meal> findByShopIdOrderByIdAsc(Long shopId);
}
