package com.pluto.takeaway.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "shop")
public class Shop {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 20)
	private String status;

	protected Shop() {

	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getStatus() {
		return status;
	}

	public void rename(String name) {
		this.name = name;
	}
}
