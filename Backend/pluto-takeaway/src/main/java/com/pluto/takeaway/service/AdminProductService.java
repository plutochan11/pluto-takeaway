package com.pluto.takeaway.service;

import com.pluto.takeaway.dto.ProductResponse;
import com.pluto.takeaway.entity.Product;
import com.pluto.takeaway.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminProductService {

	private final ShopAccessService shopAccessService;

	private final ProductRepository productRepository;

	public AdminProductService(
		  ShopAccessService shopAccessService,
		  ProductRepository productRepository) {

		this.shopAccessService = shopAccessService;
		this.productRepository = productRepository;
	}

	@Transactional
	public ProductResponse updateAvailability(
		  String username,
		  Long shopId,
		  Long productId,
		  boolean available
	) {

		shopAccessService.requireOwnedShop(username, shopId);

		Product product = productRepository.findByIdAndShopId(productId, shopId)
			  .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		product.setAvailable(available);

		return new ProductResponse(
			  product.getId(),
			  product.getName(),
			  product.getPrice(),
			  product.getStatus()
		);
	}
}
