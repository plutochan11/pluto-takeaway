package com.pluto.takeaway.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
		http
			  .authorizeHttpRequests(auth -> auth
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
			  .formLogin(AbstractHttpConfigurer::disable)
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
