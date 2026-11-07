package com.pluto.takeaway.service;

import com.pluto.takeaway.dto.*;
import com.pluto.takeaway.entity.*;
import com.pluto.takeaway.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class AdminMealService {

	private final ShopAccessService shopAccessService;
	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;
	private final MealRepository mealRepository;
	private final MealOptionGroupRepository mealOptionGroupRepository;
	private final MealOptionRepository mealOptionRepository;

	public AdminMealService(
		  ShopAccessService shopAccessService,
		  CategoryRepository categoryRepository,
		  ProductRepository productRepository,
		  MealRepository mealRepository,
		  MealOptionGroupRepository mealOptionGroupRepository,
		  MealOptionRepository mealOptionRepository) {
		this.shopAccessService = shopAccessService;
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.mealRepository = mealRepository;
		this.mealOptionGroupRepository = mealOptionGroupRepository;
		this.mealOptionRepository = mealOptionRepository;
	}

	@Transactional
	public MealResponse createMeal(
		  String username,
		  Long shopId,
		  CreateMealRequest request
	) {

		shopAccessService.requireOwnedShop(username, shopId);

		requireCategoryInShop(shopId, request.categoryId());

		Map<Long, Product> productsById = loadProductsInShop(shopId, request);

		Meal meal = mealRepository.save(
			  Meal.createUnavailable(
				    shopId,
				    request.categoryId(),
				    request.name(),
				    request.basePrice()
			  )
		);

		List<MealGroupResponse> groupResponses = new ArrayList<>();

		for (CreateMealGroupRequest groupRequest : request.groups()) {

			MealOptionGroup group = mealOptionGroupRepository.save(
				  MealOptionGroup.create(
					    shopId,
					    meal.getId(),
					    groupRequest.name(),
					    groupRequest.sortOrder()
				  )
			);

			List<MealOptionResponse> optionResponses = new ArrayList<>();

			for (CreateMealOptionRequest optionRequest : groupRequest.options()) {

				MealOption option = mealOptionRepository.save(
					  MealOption.createUnavailable(
						    shopId,
						    group.getId(),
						    optionRequest.productId(),
						    optionRequest.label(),
						    optionRequest.supplement(),
						    optionRequest.sortOrder()
					  )
				);

				String displayName;

				if (option.getProductId() == null) {
					displayName = option.getLabel();
				} else {
					displayName = productsById
						  .get(option.getProductId())
						  .getName();
				}

				optionResponses.add(
					  new MealOptionResponse(
						    option.getId(),
						    option.getProductId(),
						    displayName,
						    option.getSupplement(),
						    option.getStatus(),
						    option.getSortOrder()
					  )
				);
			}

			optionResponses.sort(
				  Comparator.comparing(MealOptionResponse::sortOrder)
					    .thenComparing(MealOptionResponse::id)
			);

			groupResponses.add(
				  new MealGroupResponse(
					    group.getId(),
					    group.getName(),
					    group.getSortOrder(),
					    optionResponses
				  )
			);
		}

		groupResponses.sort(
			  Comparator.comparing(MealGroupResponse::sortOrder)
				    .thenComparing(MealGroupResponse::id)
		);

		return new MealResponse(
			  meal.getId(),
			  meal.getShopId(),
			  meal.getCategoryId(),
			  meal.getName(),
			  meal.getBasePrice(),
			  meal.getStatus(),
			  groupResponses
		);
	}

	private void requireCategoryInShop(Long shopId, Long categoryId) {

		Category category = categoryRepository.findById(categoryId)
			  .orElseThrow(() ->
				    new ResponseStatusException(HttpStatus.NOT_FOUND)
			  );

		if (!Objects.equals(category.getShopId(), shopId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

	private Map<Long, Product> loadProductsInShop(
		  Long shopId,
		  CreateMealRequest request
	) {

		Set<Long> productIds = new HashSet<>();

		for (CreateMealGroupRequest group : request.groups()) {
			for (CreateMealOptionRequest option : group.options()) {
				if (option.productId() != null) {
					productIds.add(option.productId());
				}
			}
		}

		Map<Long, Product> productsById = new HashMap<>();

		if (productIds.isEmpty()) {
			return productsById;
		}

		List<Product> products = productRepository.findAllById(productIds);

		if (products.size() != productIds.size()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		for (Product product : products) {
			if (!Objects.equals(product.getShopId(), shopId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN);
			}

			productsById.put(product.getId(), product);
		}

		return productsById;
	}
}
