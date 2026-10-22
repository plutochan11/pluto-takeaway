package com.pluto.takeaway.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "category")
public class Category {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "shop_id", nullable = false)
	private Long shopId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	protected Category() {
	}

	public Long getId() {
		return id;
	}

	public Long getShopId() {
		return shopId;
	}

	public String getName() {
		return name;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}
}
