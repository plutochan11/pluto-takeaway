package com.pluto.takeaway.dto;

public record CsrfResponse(
	  String headerName,
	  String parameterName,
	  String token
) {
}
