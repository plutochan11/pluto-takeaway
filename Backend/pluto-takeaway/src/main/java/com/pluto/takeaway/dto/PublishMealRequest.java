package com.pluto.takeaway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PublishMealRequest(
	  @NotEmpty
	  @Size(max = 1000)
	  List<@NotNull @Positive Long> optionIdsToEnable
) {
}
