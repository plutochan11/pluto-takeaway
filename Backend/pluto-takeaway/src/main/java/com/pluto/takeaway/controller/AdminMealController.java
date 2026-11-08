package com.pluto.takeaway.controller;

import com.pluto.takeaway.dto.CreateMealRequest;
import com.pluto.takeaway.dto.MealResponse;
import com.pluto.takeaway.dto.PublishMealRequest;
import com.pluto.takeaway.service.AdminMealService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdminMealController {

	private final AdminMealService adminMealService;

	public AdminMealController(AdminMealService adminMealService) {
		this.adminMealService = adminMealService;
	}

	@PostMapping(
		  value = "/api/admin/shops/{shopId}/meals",
		  consumes = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<MealResponse> createMeal(
		  @PathVariable("shopId") Long shopId,
		  @Valid @RequestBody CreateMealRequest request,
		  Authentication authentication
	) {

		MealResponse response = adminMealService.createMeal(
			  authentication.getName(),
			  shopId,
			  request
		);

		return ResponseEntity
			  .status(HttpStatus.CREATED)
			  .body(response);
	}

	@PutMapping(
		  value = "/api/admin/shops/{shopId}/meals/{mealId}/publication",
		  consumes = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<Void> publishMeal(
		  @PathVariable("shopId") Long shopId,
		  @PathVariable("mealId") Long mealId,
		  @RequestBody @Valid PublishMealRequest request,
		  Authentication authentication
	) {

		adminMealService.publishMeal(
			  authentication.getName(),
			  shopId,
			  mealId,
			  request
		);

		return ResponseEntity.noContent().build();
	}
}
