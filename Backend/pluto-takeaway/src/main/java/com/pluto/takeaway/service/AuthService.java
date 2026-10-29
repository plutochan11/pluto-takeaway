package com.pluto.takeaway.service;

import com.pluto.takeaway.dto.CurrentUserResponse;
import com.pluto.takeaway.entity.UserAccount;
import com.pluto.takeaway.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private final UserAccountRepository userAccountRepository;

	public AuthService(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional(readOnly = true)
	public CurrentUserResponse getCurrentUser(String username) {

		UserAccount userAccount = userAccountRepository.findByUsername(username)
			  .orElseThrow(() ->
				    new ResponseStatusException(HttpStatus.UNAUTHORIZED)
			  );

		return
			  new CurrentUserResponse(
				    userAccount.getId(),
				    userAccount.getUsername(),
				    userAccount.getRole(),
				    userAccount.getShopId()
			  );
	}
}
