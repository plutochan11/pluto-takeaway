package com.pluto.takeaway.controller;

import com.pluto.takeaway.dto.CsrfResponse;
import com.pluto.takeaway.dto.CurrentUserResponse;
import com.pluto.takeaway.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@GetMapping("/api/auth/csrf")
	public CsrfResponse csrf(CsrfToken csrfToken) {
		return new CsrfResponse(
			  csrfToken.getHeaderName(),
			  csrfToken.getParameterName(),
			  csrfToken.getToken()
		);
	}

	@GetMapping("/api/auth/me")
	public CurrentUserResponse me(Authentication authentication) {

		String username = authentication.getName();

		return authService.getCurrentUser(username);
	}
}
