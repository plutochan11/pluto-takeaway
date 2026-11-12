package com.pluto.takeaway.repository;

import com.pluto.takeaway.entity.Meal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface MealRepository extends JpaRepository<Meal, Long> {

	List<Meal> findByShopIdOrderByIdAsc(Long shopId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Meal> findByIdAndShopId(Long id, Long shopId);

	List<Meal> findByShopIdAndStatusOrderByIdAsc(Long shopId, String status);
}
