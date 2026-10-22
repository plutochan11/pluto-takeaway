package com.pluto.takeaway.service;

import com.pluto.takeaway.dto.CategoryResponse;
import com.pluto.takeaway.dto.MenuResponse;
import com.pluto.takeaway.dto.ProductResponse;
import com.pluto.takeaway.dto.ShopResponse;
import com.pluto.takeaway.entity.Category;
import com.pluto.takeaway.entity.Product;
import com.pluto.takeaway.entity.Shop;
import com.pluto.takeaway.repository.CategoryRepository;
import com.pluto.takeaway.repository.ProductRepository;
import com.pluto.takeaway.repository.ShopRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShopService {
	private final ShopRepository shopRepository;
	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	public ShopService(
		  ShopRepository shopRepository,
		  CategoryRepository categoryRepository,
		  ProductRepository productRepository) {
		this.shopRepository = shopRepository;
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public List<ShopResponse> getShops() {
		List<Shop> shops = shopRepository.findAll(Sort.by("id").ascending());

		List<ShopResponse> responses = new ArrayList<>();

		for (Shop shop : shops) {
			ShopResponse shopResponse = new ShopResponse(
				  shop.getId(),
				  shop.getName(),
				  shop.getStatus()
			);

			responses.add(shopResponse);
		}

		return responses;
	}

	@Transactional(readOnly = true)
	public ShopResponse getShop(Long shopId) {
		Shop shop = shopRepository.findById(shopId)
			  .orElseThrow(() ->
				    new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Shop not found"
				    )
			  );

		return new ShopResponse(
			  shop.getId(),
			  shop.getName(),
			  shop.getStatus()
		);
	}

	@Transactional(readOnly = true)
	public MenuResponse getMenu(Long shopId) {
		// Query for shop
		Shop shop = shopRepository.findById(shopId)
			  .orElseThrow(() ->
				    new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Shop not found"
				    )
			  );

		// Map to shop response
		ShopResponse shopResponse = new ShopResponse(
			  shop.getId(),
			  shop.getName(),
			  shop.getStatus()
		);

		// Query for all categories
		List<Category> categories = categoryRepository.findByShopIdOrderBySortOrderAscIdAsc(shopId);

		// Query for all products
		List<Product> products = productRepository.findByShopIdOrderByIdAsc(shopId);

		// Map categories into category responses
		List<CategoryResponse> categoryResponses = getCategoryResponses(categories, products);

		// Return menu response
		return new MenuResponse(
			  shopResponse,
			  categoryResponses
		);
	}

	private static @NonNull List<CategoryResponse> getCategoryResponses(List<Category> categories, List<Product> products) {
		List<CategoryResponse> categoryResponses = new ArrayList<>();

		for (Category category : categories) {
			// Add products that have same category ID to individual lists
			List<ProductResponse> productResponses = new ArrayList<>();

			for (Product product : products) {
				if (category.getId().equals(product.getCategoryId())) {
					productResponses.add(
						  new ProductResponse(
							    product.getId(),
							    product.getName(),
							    product.getPrice(),
							    product.getStatus()
						  )
					);
				}
			}

			categoryResponses.add(
				  new CategoryResponse(
					    category.getId(),
					    category.getName(),
						productResponses
					    )
			);
		}
		return categoryResponses;
	}
}
