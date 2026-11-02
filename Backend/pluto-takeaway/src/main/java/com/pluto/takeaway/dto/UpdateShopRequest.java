package com.pluto.takeaway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateShopRequest(
	  @NotBlank
	  @Size(max = 100)
	  String name
) {
}
