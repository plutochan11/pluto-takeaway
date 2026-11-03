package com.pluto.takeaway.exception;

import com.pluto.takeaway.dto.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(
		  MethodArgumentNotValidException exception
	) {

		return ResponseEntity.badRequest().body(
			  new ApiErrorResponse(
				    "VALIDATION_FAILED",
				    "Request validation failed"
			  )
		);
	}

	@ExceptionHandler({
		  HttpMessageNotReadableException.class,
		  MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
		  Exception exception
	) {

		return ResponseEntity.badRequest().body(
			  new ApiErrorResponse(
				    "VALIDATION_FAILED",
				    "Request input is invalid"
			  )
		);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatus(
		  ResponseStatusException exception
	) {

		int status = exception.getStatusCode().value();

		ApiErrorResponse body = null;

		switch (status) {
			case 401:
				body = new ApiErrorResponse(
					  "AUTHENTICATION_REQUIRED",
					  "Authentication is required"
				);
				break;

			case 403:
				body = new ApiErrorResponse(
					  "ACCESS_DENIED",
					  "Access is denied"
				);
				break;

			case 404:
				body = new ApiErrorResponse(
					  "RESOURCE_NOT_FOUND",
					  "Resource not found"
				);
				break;

			default:
				throw exception;
		}

		return ResponseEntity.status(status)
			  .body(body);
	}
}
