package com.pluto.takeaway.dto;

import java.math.BigDecimal;
import java.util.List;

public record MenuMealResponse(
	  Long id,
	  String name,
	  BigDecimal basePrice,
	  boolean configurable,
	  List<MenuMealGroupResponse> groups
) {
}
