package com.pluto.takeaway.dto;

public record ApiErrorResponse(
	  String code,
	  String message
) {
}
