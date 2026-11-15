package com.pluto.takeaway.controller;

import com.pluto.takeaway.dto.ProductResponse;
import com.pluto.takeaway.dto.UpdateProductAvailabilityRequest;
import com.pluto.takeaway.service.AdminProductService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminProductController {

	private final AdminProductService adminProductService;

	public AdminProductController(AdminProductService adminProductService) {

		this.adminProductService = adminProductService;
	}

	@PatchMapping(
		  value = "/api/admin/shops/{shopId}/products/{productId}/availability",
		  consumes = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<ProductResponse> updateProductAvailable(
		  @PathVariable("shopId") Long shopId,
		  @PathVariable("productId") Long productId,
		  @RequestBody @Valid UpdateProductAvailabilityRequest request,
		  Authentication authentication
	) {

		ProductResponse response = adminProductService.updateAvailability(
			  authentication.getName(),
			  shopId,
			  productId,
			  "AVAILABLE".equals(request.status())
		);

		return ResponseEntity.ok(response);
	}
}
