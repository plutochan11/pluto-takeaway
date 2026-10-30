package com.pluto.takeaway.controller;

import com.pluto.takeaway.dto.ShopResponse;
import com.pluto.takeaway.service.AdminShopService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminShopController {

	private final AdminShopService adminShopService;


	public AdminShopController(AdminShopService adminShopService) {
		this.adminShopService = adminShopService;
	}

	@GetMapping("/api/admin/shops/{shopId}")
	public ShopResponse getOwnShop(
		  @PathVariable("shopId") Long shopId,
		  Authentication authentication
	) {

		return adminShopService.getOwnShop(
			  authentication.getName(),
			  shopId
		);
	}
}
