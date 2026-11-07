package com.pluto.takeaway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateMealRequest(
	  @NotNull
	  @Positive
	  Long categoryId,

	  @NotBlank
	  @Size(max = 100)
	  String name,

	  @NotNull
	  @DecimalMin("0.00")
	  @Digits(integer = 4, fraction = 2)
	  BigDecimal basePrice,

	  @NotEmpty
	  @Size(max = 20)
	  List<@NotNull @Valid CreateMealGroupRequest> groups
) {
}
