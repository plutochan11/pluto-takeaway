package com.pluto.takeaway.repository;

import com.pluto.takeaway.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, Long> {
}
