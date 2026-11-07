package com.pluto.takeaway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateMealGroupRequest(
	  @NotBlank
	  @Size(max = 50)
	  String name,

	  @NotNull
	  @PositiveOrZero
	  Integer sortOrder,

	  @NotEmpty
	  @Size(max = 50)
	  List<@NotNull @Valid CreateMealOptionRequest> options
) {
}
