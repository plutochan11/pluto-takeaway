package com.pluto.takeaway.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_account")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "username", nullable = false, length = 50, unique = true)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "role", nullable = false, length = 20)
	private String role;

	@Column(name = "enabled", nullable = false)
	private Boolean enabled;

	@Column(name = "shop_id")
	private Long shopId;

	protected UserAccount() {}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getRole() {
		return role;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public Long getShopId() {
		return shopId;
	}
}
