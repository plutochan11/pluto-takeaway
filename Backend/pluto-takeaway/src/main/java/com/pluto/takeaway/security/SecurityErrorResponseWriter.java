package com.pluto.takeaway.security;

import com.pluto.takeaway.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
public class SecurityErrorResponseWriter {

	private final JsonMapper jsonMapper;

	public SecurityErrorResponseWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void write(
		  HttpServletResponse response,
		  int status,
		  String code,
		  String message
	) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		jsonMapper.writeValue(
			  response.getWriter(),
			  new ApiErrorResponse(code, message)
		);
	}
}
