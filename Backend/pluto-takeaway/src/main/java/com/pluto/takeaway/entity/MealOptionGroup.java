package com.pluto.takeaway.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "meal_option_group")
public class MealOptionGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "shop_id", nullable = false)
	private Long shopId;

	@Column(name = "meal_id", nullable = false)
	private Long mealId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	protected MealOptionGroup() {
	}

	public Long getId() {
		return id;
	}

	public Long getShopId() {
		return shopId;
	}

	public Long getMealId() {
		return mealId;
	}

	public String getName() {
		return name;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}
}
