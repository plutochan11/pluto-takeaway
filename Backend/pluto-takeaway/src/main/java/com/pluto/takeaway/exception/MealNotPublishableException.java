package com.pluto.takeaway.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MealNotPublishableException extends ResponseStatusException {
	public MealNotPublishableException(String reason) {
		super(HttpStatus.CONFLICT, reason);
	}
}
