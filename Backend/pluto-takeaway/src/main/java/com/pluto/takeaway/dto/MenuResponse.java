package com.pluto.takeaway.dto;

import java.util.List;

public record MenuResponse(
	  ShopResponse shop,
	  List<CategoryResponse> categories) {
}
