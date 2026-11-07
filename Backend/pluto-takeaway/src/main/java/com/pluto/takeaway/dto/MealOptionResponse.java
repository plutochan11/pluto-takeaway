package com.pluto.takeaway.dto;

import java.math.BigDecimal;

public record MealOptionResponse(
	  Long id,
	  Long productId,

	  // The display name
	  // Will use the referenced product's name for a product-backed option
	  // Will use the label for a non-product option
	  String name,
	  
	  BigDecimal supplement,
	  String status,
	  Integer sortOrder
) {
}
