package com.pluto.takeaway.repository;

import com.pluto.takeaway.entity.MealOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealOptionRepository extends JpaRepository<MealOption, Long> {

	List<MealOption> findByShopIdAndGroupIdOrderBySortOrderAscIdAsc(Long shopId, Long groupId);
}
