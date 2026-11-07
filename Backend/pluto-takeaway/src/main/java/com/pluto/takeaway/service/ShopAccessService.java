package com.pluto.takeaway.service;

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
public class ShopAccessService {

	private final UserAccountRepository userAccountRepository;

	private final ShopRepository shopRepository;

	public ShopAccessService(UserAccountRepository userAccountRepository, ShopRepository shopRepository) {
		this.userAccountRepository = userAccountRepository;
		this.shopRepository = shopRepository;
	}

	/**
	 * Returns the requested shop if the account is its SHOP_ADMIN
	 *
	 * @throws ResponseStatusException with status 401 if the account is missing,
	 *                                 403 if the account is not a shop admin or owns another shop,
	 *                                 or 404 if the owned shop is missing
	 */
	@Transactional(readOnly = true)
	public Shop requireOwnedShop(String username, Long shopId) {

		// Find user account with username
		// Return 401 if not found
		UserAccount userAccount = userAccountRepository.findByUsername(username)
			  .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

		// Check if the account role is SHOP_ADMIN
		// Return 403 if not
		if (!Objects.equals("SHOP_ADMIN", userAccount.getRole())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		// Check if they own this shop
		// Return 403 if not
		if (!Objects.equals(shopId, userAccount.getShopId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		// Find shop
		// Return 404 if not found
		return shopRepository.findById(userAccount.getShopId())
			  .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
}
