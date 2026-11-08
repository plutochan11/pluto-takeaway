package com.pluto.takeaway.service;

import com.pluto.takeaway.dto.*;
import com.pluto.takeaway.entity.*;
import com.pluto.takeaway.exception.MealNotPublishableException;
import com.pluto.takeaway.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

	@Transactional(isolation = Isolation.READ_COMMITTED)
	public void publishMeal(
		  String username,
		  Long shopId,
		  Long mealId,
		  PublishMealRequest request
	) {

		shopAccessService.requireOwnedShop(username, shopId);

		// Find shop by ID and ensure it belongs to the shop
		Meal meal = mealRepository.findByIdAndShopId(mealId, shopId)
			  .orElseThrow(() ->
				    new ResponseStatusException(HttpStatus.NOT_FOUND));

		Set<Long> enabledOptionIds = new HashSet<>(request.enabledOptionIds());

		// Check for duplicate option IDs
		if (enabledOptionIds.size() != request.enabledOptionIds().size()) {
			throw new ResponseStatusException(
				  HttpStatus.BAD_REQUEST,
				  "Duplicate option IDs"
			);
		}

		List<Long> groupIds = mealOptionGroupRepository
			  .findByShopIdAndMealIdOrderBySortOrderAscIdAsc(shopId, mealId)
			  .stream()
			  .map(MealOptionGroup::getId)
			  .toList();

		if (groupIds.isEmpty()) {
			throw new MealNotPublishableException("The meal has no option groups");
		}

		// Find options that belong to the meal group of this shop.
		List<MealOption> options = mealOptionRepository.findByShopIdAndGroupIdIn(shopId, groupIds);

		List<MealOption> selectedOptions = options.stream()
			  .filter(option -> enabledOptionIds.contains(option.getId()))
			  .toList();

		// Check that all selected options belong to this meal
		if (selectedOptions.size() != enabledOptionIds.size()) {
			throw new ResponseStatusException(
				  HttpStatus.BAD_REQUEST,
				  "All selected options must belong to this meal"
			);
		}

		Set<Long> selectedOptionsProductIds = new HashSet<>();

		for (MealOption option : selectedOptions) {
			if (option.getProductId() != null) {
				selectedOptionsProductIds.add(option.getProductId());
			}
		}

		Set<Long> availableProductIds = new HashSet<>();

		if (!selectedOptionsProductIds.isEmpty()) {
			for (Product product : productRepository.findAllById(selectedOptionsProductIds)) {
				if ("AVAILABLE".equals(product.getStatus())) {
					availableProductIds.add(product.getId());
				}
			}
		}

		if (!availableProductIds.containsAll(selectedOptionsProductIds)) {
			throw new MealNotPublishableException(
				  "All selected product-backed options must reference available products");
		}

		Set<Long> coveredGroupIds = new HashSet<>();
		for (MealOption option : selectedOptions) {
			coveredGroupIds.add(option.getGroupId());
		}

		if (!coveredGroupIds.containsAll(groupIds)) {
			throw new MealNotPublishableException("Each group needs a selectable option");
		}

		for (MealOption option : options) {
			option.setEnabled(enabledOptionIds.contains(option.getId()));
		}

		meal.publish();
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
