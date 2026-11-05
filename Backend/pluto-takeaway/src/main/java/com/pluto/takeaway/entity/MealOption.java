package com.pluto.takeaway.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "meal_option")
public class MealOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "shop_id", nullable = false)
	private Long shopId;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "product_id")
	private Long productId;

	@Column(name = "label", length = 100)
	private String label;

	@Column(name = "supplement", nullable = false, precision = 10, scale = 2)
	private BigDecimal supplement;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	protected MealOption() {
	}

	public Long getId() {
		return id;
	}

	public Long getShopId() {
		return shopId;
	}

	public Long getGroupId() {
		return groupId;
	}

	public Long getProductId() {
		return productId;
	}

	public String getLabel() {
		return label;
	}

	public BigDecimal getSupplement() {
		return supplement;
	}

	public String getStatus() {
		return status;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}
}
