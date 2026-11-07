package com.pluto.takeaway.service;

import com.pluto.takeaway.dto.ShopResponse;
import com.pluto.takeaway.entity.Shop;
import com.pluto.takeaway.repository.ShopRepository;
import com.pluto.takeaway.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminShopService {

	private final ShopAccessService shopAccessService;

	public AdminShopService(ShopAccessService shopAccessService) {
		this.shopAccessService = shopAccessService;
	}

	@Transactional(readOnly = true)
	public ShopResponse getOwnShop(String username, Long shopId) {

		Shop shop = shopAccessService.requireOwnedShop(username, shopId);

		return new ShopResponse(
			  shop.getId(),
			  shop.getName(),
			  shop.getStatus()
		);
	}

	@Transactional
	public ShopResponse updateShopName(
		  String username,
		  Long shopId,
		  String name
	) {

		Shop shop = shopAccessService.requireOwnedShop(username, shopId);

		shop.rename(name);

		return new ShopResponse(
			  shop.getId(),
			  shop.getName(),
			  shop.getStatus()
		);
	}


}
