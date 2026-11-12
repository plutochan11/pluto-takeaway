package com.pluto.takeaway.dto;

import java.util.List;

public record MenuMealGroupResponse(
	  Long id,
	  String name,
	  List<MenuMealOptionResponse> options
) {
}
