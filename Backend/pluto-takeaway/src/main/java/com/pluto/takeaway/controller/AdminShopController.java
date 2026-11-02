package com.pluto.takeaway.controller;

import com.pluto.takeaway.dto.ShopResponse;
import com.pluto.takeaway.dto.UpdateShopRequest;
import com.pluto.takeaway.service.AdminShopService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

	@PatchMapping("/api/admin/shops/{shopId}")
	public ShopResponse updateShopName(
		  @PathVariable("shopId") Long shopId,
		  @Valid @RequestBody UpdateShopRequest request,
		  Authentication authentication
	) {

		return adminShopService.updateShopName(
			  authentication.getName(),
			  shopId,
			  request.name()
		);
	}
}
