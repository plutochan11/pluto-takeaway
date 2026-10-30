package com.pluto.takeaway.service;

import com.pluto.takeaway.dto.ShopResponse;
import com.pluto.takeaway.entity.Shop;
import com.pluto.takeaway.entity.UserAccount;
import com.pluto.takeaway.repository.ShopRepository;
import com.pluto.takeaway.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class AdminShopService {

	private final UserAccountRepository userAccountRepository;

	private final ShopRepository shopRepository;

	public AdminShopService(UserAccountRepository userAccountRepository, ShopRepository shopRepository) {
		this.userAccountRepository = userAccountRepository;
		this.shopRepository = shopRepository;
	}

	@Transactional(readOnly = true)
	public ShopResponse getOwnShop(String username, Long shopId) {

		// Find user account with username
		// Return 401 if not found
		UserAccount userAccount = userAccountRepository.findByUsername(username)
			  .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

		// Check if account role is SHOP_ADMIN
		// Return 403 if not
		if (!Objects.equals("SHOP_ADMIN", userAccount.getRole())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		if (!Objects.equals(shopId, userAccount.getShopId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		// Find shop
		Shop shop = shopRepository.findById(userAccount.getShopId())
			  .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		return new ShopResponse(
			  shop.getId(),
			  shop.getName(),
			  shop.getStatus()
		);
	}
}
