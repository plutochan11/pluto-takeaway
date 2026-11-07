package com.pluto.takeaway.dto;

import java.math.BigDecimal;
import java.util.List;

public record MealResponse(
	  Long id,
	  Long shopId,
	  Long categoryId,
	  String name,
	  BigDecimal basePrice,
	  String status,
	  List<MealGroupResponse> groups
) {
}
