package com.pluto.takeaway.repository;

import com.pluto.takeaway.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
	public List<Product> findByShopIdOrderByIdAsc(Long shopId);
}
