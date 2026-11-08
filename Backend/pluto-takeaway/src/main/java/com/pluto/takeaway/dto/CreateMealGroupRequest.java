package com.pluto.takeaway.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	@AssertTrue(message = "A product can appear only once in an option group")
	@JsonIgnore
	public boolean isProductSelectionUnique() {
		if (options == null) {
			return true;
		}

		Set<Long> productIds = new HashSet<>();
		
		for (CreateMealOptionRequest option : options) {
			if (option != null && option.productId() != null
				  && !productIds.add(option.productId())) {
				return false;
			}
		}
		return true;
	}
}
