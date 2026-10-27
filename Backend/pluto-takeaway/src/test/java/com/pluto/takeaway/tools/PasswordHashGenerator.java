package com.pluto.takeaway.tools;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordHashGenerator {
	public static void main(String[] args) {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

		for (int i = 0; i < 4; i++) {
			System.out.println(encoder.encode("password"));
		}

//		String hash = encoder.encode("DemoOnly123!");
//
//		System.out.println(hash);
//		System.out.println(encoder.matches("DemoOnly123!", hash));
//		System.out.println(encoder.matches("WrongPassword", hash));
	}
}
