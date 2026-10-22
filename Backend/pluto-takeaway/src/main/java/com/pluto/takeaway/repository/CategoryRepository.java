package com.pluto.takeaway.repository;

import com.pluto.takeaway.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
	List<Category> findByShopIdOrderBySortOrderAscIdAsc(Long shopId);
}
