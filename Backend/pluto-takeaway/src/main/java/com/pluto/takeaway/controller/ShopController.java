package com.pluto.takeaway.controller;

import com.pluto.takeaway.dto.MenuResponse;
import com.pluto.takeaway.dto.ShopResponse;
import com.pluto.takeaway.service.ShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ShopController {

	private final ShopService shopService;

	public ShopController(ShopService shopService) {
		this.shopService = shopService;
	}

	@GetMapping("/api/shops")
	public List<ShopResponse> getShops() {
		return shopService.getShops();
	}

	@GetMapping("/api/shops/{shopId}")
	public ShopResponse getShop(@PathVariable("shopId") Long shopId) {
		return shopService.getShop(shopId);
	}

	@GetMapping("/api/shops/{shopId}/menu")
	public MenuResponse getMenu(@PathVariable("shopId") Long shopId) {
		return shopService.getMenu(shopId);
	}
}
