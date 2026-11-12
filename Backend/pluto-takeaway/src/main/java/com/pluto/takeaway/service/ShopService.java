package com.pluto.takeaway.service;

import com.pluto.takeaway.dto.*;
import com.pluto.takeaway.entity.*;
import com.pluto.takeaway.repository.*;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShopService {

	private final ShopRepository shopRepository;
	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	private final MealRepository mealRepository;

	private final MealOptionGroupRepository mealOptionGroupRepository;

	private final MealOptionRepository mealOptionRepository;

	public ShopService(
		  ShopRepository shopRepository,
		  CategoryRepository categoryRepository,
		  ProductRepository productRepository,
		  MealRepository mealRepository,
		  MealOptionGroupRepository mealOptionGroupRepository,
		  MealOptionRepository mealOptionRepository) {

		this.shopRepository = shopRepository;
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.mealRepository = mealRepository;
		this.mealOptionGroupRepository = mealOptionGroupRepository;
		this.mealOptionRepository = mealOptionRepository;
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

		// Find the shop by ID
		Shop shop = shopRepository.findById(shopId)
			  .orElseThrow(() ->
				    new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Shop not found"
				    )
			  );

		// Assemble ShopResponse
		ShopResponse shopResponse = new ShopResponse(
			  shop.getId(),
			  shop.getName(),
			  shop.getStatus()
		);

		// Find all categories in this shop
		List<Category> categories = categoryRepository.findByShopIdOrderBySortOrderAscIdAsc(shopId);

		// Find all products in this shop
		List<Product> products = productRepository.findByShopIdOrderByIdAsc(shopId);

		// Get MenuMealResponses by category ID
		Map<Long, List<MenuMealResponse>> mealsByCategory = loadMenuMealsByCategory(shopId, products);

		// Assemble CategoryResponse
		List<CategoryResponse> categoryResponses = getCategoryResponses(
			  categories,
			  products,
			  mealsByCategory
		);

		return new MenuResponse(
			  shopResponse,
			  categoryResponses
		);
	}

	private static @NonNull List<CategoryResponse> getCategoryResponses(
		  List<Category> categories,
		  List<Product> products,
		  Map<Long, List<MenuMealResponse>> mealsByCategory
	) {

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
					    productResponses,
					    mealsByCategory.getOrDefault(category.getId(), List.of())
				  )
			);
		}
		return categoryResponses;
	}

	/**
	 * Assemble {@link MenuMealResponse} by category ID
	 */
	private Map<Long, List<MenuMealResponse>> loadMenuMealsByCategory(
		  Long shopId,
		  List<Product> products
	) {

		// Find available meals from the shop
		List<Meal> meals = mealRepository.findByShopIdAndStatusOrderByIdAsc(shopId, "AVAILABLE");

		if (meals.isEmpty()) {
			return Map.of();
		}

		List<Long> mealIds = meals.stream().map(Meal::getId).toList();

		// Find meal groups
		List<MealOptionGroup> groups = mealOptionGroupRepository.findByShopIdAndMealIdInOrderBySortOrderAscIdAsc(
			  shopId,
			  mealIds
		);

		Map<Long, List<MealOptionGroup>> groupsByMeal = groups.stream()
			  .collect(Collectors.groupingBy(MealOptionGroup::getMealId));

		List<Long> groupIds = groups.stream().map(MealOptionGroup::getId).toList();

		// Find meal options
		List<MealOption> options = groupIds.isEmpty()
			  ? List.of()
			  : mealOptionRepository.findByShopIdAndGroupIdIn(
			  shopId,
			  groupIds
		);

		Map<Long, List<MealOption>> enabledOptionsByGroup = options.stream()
			  .filter(option -> "AVAILABLE".equals(option.getStatus()))
			  .sorted(Comparator.comparing(MealOption::getSortOrder)
				    .thenComparing(MealOption::getId))
			  .collect(Collectors.groupingBy(MealOption::getGroupId));

		Map<Long, Product> productsById = products.stream()
			  .collect(Collectors.toMap(Product::getId, product -> product));

		Map<Long, List<MenuMealResponse>> mealsByCategory = new HashMap<>();

		for (Meal meal : meals) {

			List<MenuMealGroupResponse> groupResponses = new ArrayList<>();

			for (MealOptionGroup group : groupsByMeal.getOrDefault(meal.getId(), List.of())) {

				List<MenuMealOptionResponse> optionResponses = enabledOptionsByGroup
					  .getOrDefault(group.getId(), List.of())
					  .stream()
					  .map(option -> toMenuMealOption(option, productsById))
					  .toList();

				groupResponses.add(new MenuMealGroupResponse(
					  group.getId(), group.getName(), optionResponses
				));
			}

			boolean configurable = !groupResponses.isEmpty()
				  && groupResponses.stream().allMatch(group ->
				  group.options().stream().anyMatch(MenuMealOptionResponse::selectable)
			);

			mealsByCategory.computeIfAbsent(meal.getCategoryId(), id -> new ArrayList<>())
				  .add(new MenuMealResponse(
					    meal.getId(), meal.getName(), meal.getBasePrice(),
					    configurable, groupResponses
				  ));
		}

		return mealsByCategory;
	}

	private static MenuMealOptionResponse toMenuMealOption(
		  MealOption option,
		  Map<Long, Product> productsById) {

		// The option's selectable by default if there's no backed product
		if (option.getProductId() == null) {

			return new MenuMealOptionResponse(
				  option.getId(), option.getLabel(), option.getSupplement(), true
			);
		}

		Product product = productsById.get(option.getProductId());

		// Determine option's selectability by backed product's availability
		return new MenuMealOptionResponse(
			  option.getId(),
			  product.getName(),
			  option.getSupplement(),
			  "AVAILABLE".equals(product.getStatus())
		);
	}
}
