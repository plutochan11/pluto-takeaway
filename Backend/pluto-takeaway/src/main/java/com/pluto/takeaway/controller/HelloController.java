package com.pluto.takeaway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	@GetMapping("/api/hello")
	public String hello() {
		return "Pluto Takeaway is running.";
	}
}
