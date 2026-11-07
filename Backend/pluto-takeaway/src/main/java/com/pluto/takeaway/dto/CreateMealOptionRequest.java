package com.pluto.takeaway.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateMealOptionRequest(
	  @Positive
	  Long productId,

	  @Size(max = 100)
	  String label,

	  @NotNull
	  @DecimalMin("0.00")
	  @Digits(integer = 4, fraction = 2)
	  BigDecimal supplement,

	  @NotNull
	  @PositiveOrZero
	  Integer sortOrder
) {

	@AssertTrue(message = "Invalid product, label or supplement")
	@JsonIgnore
	public boolean isValidOptionConfiguration() {

		if (productId != null) {
			return label == null;
		}

		return label != null
			  && !label.isBlank()
			  && supplement != null
			  && supplement.signum() == 0;
	}
}
