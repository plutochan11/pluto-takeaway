package com.pluto.takeaway.dto;

import java.math.BigDecimal;

public record MenuMealOptionResponse(
	  Long id,
	  String name,
	  BigDecimal supplement,
	  boolean selectable
) {
}
