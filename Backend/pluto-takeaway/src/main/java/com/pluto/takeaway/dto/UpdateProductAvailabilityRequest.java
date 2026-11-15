package com.pluto.takeaway.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateProductAvailabilityRequest(
	  @NotNull
	  @Pattern(regexp = "AVAILABLE|UNAVAILABLE")
	  String status
) {
}
