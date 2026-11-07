package com.pluto.takeaway.dto;

import java.util.List;

public record MealGroupResponse(
	  Long id,
	  String name,
	  Integer sortOrder,
	  List<MealOptionResponse> options
) {
}
