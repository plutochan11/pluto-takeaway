package com.pluto.takeaway.dto;

public record CurrentUserResponse(
	  Long id,
	  String username,
	  String role,
	  Long shopId
) {
}
