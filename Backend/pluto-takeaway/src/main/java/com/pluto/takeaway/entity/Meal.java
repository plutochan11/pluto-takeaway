package com.pluto.takeaway.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "meal")
public class Meal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "shop_id", nullable = false)
	private Long shopId;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "base_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal basePrice;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	protected Meal() {
	}

	public Long getId() {
		return id;
	}

	public Long getShopId() {
		return shopId;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getBasePrice() {
		return basePrice;
	}

	public String getStatus() {
		return status;
	}
}
