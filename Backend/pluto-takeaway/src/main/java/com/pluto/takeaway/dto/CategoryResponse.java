package com.pluto.takeaway.dto;

import java.util.List;

public record CategoryResponse(Long id, String name, List<ProductResponse> products) {
}
