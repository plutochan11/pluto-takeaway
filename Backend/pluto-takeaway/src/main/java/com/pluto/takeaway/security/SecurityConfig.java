package com.pluto.takeaway.security;

import com.pluto.takeaway.dto.ApiErrorResponse;
import com.pluto.takeaway.dto.CurrentUserResponse;
import com.pluto.takeaway.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(
		  HttpSecurity http,
		  AuthService authService,
		  JsonMapper jsonMapper
	) throws Exception {
		http
			  .authorizeHttpRequests(auth -> auth
				    .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				    .requestMatchers(
						HttpMethod.GET,
					      "/api/hello",
					      "/api/shops",
					      "/api/shops/*",
					      "/api/shops/*/menu",
					      "/api/auth/csrf"
				  ).permitAll()
				  .anyRequest().authenticated()
			  )
			  .httpBasic(AbstractHttpConfigurer::disable)
//			  .formLogin(AbstractHttpConfigurer::disable)

			  .formLogin(form -> form
				    .loginProcessingUrl("/api/auth/login")
				    .successHandler((request, response, authentication) -> {
					    CurrentUserResponse currentUser =
						      authService.getCurrentUser(authentication.getName());

					    response.setStatus(HttpServletResponse.SC_OK);
					    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					    response.setCharacterEncoding("UTF-8");

					    jsonMapper.writeValue(response.getWriter(), currentUser);
				    })
				    .failureHandler((request, response, exception) -> {
					    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					    response.setCharacterEncoding("UTF-8");

					    jsonMapper.writeValue(
							response.getWriter(),
						      new ApiErrorResponse(
								  "INVALID_CREDENTIALS",
							        "Invalid username or password"
						      )
					    );
				    })
				    .permitAll()
			  )
			  .exceptionHandling(errors -> errors
				    .authenticationEntryPoint(
					      (request, response, exception) ->
						        response.setStatus(
							          HttpServletResponse.SC_UNAUTHORIZED
						        )
				    )
			  );

		return http.build();
	}
}
