package com.pluto.takeaway.repository;

import com.pluto.takeaway.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

	List<Product> findByShopIdOrderByIdAsc(Long shopId);

	Optional<Product> findByIdAndShopId(Long id, Long shopId);
}
