package com.pluto.takeaway.controller;

import com.pluto.takeaway.dto.CsrfResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

	@GetMapping("/api/auth/csrf")
	public CsrfResponse csrf(CsrfToken csrfToken) {
		return new CsrfResponse(
			  csrfToken.getHeaderName(),
			  csrfToken.getParameterName(),
			  csrfToken.getToken()
		);
	}
}
