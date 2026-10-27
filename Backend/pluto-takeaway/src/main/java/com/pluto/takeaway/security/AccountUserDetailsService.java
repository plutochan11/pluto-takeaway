package com.pluto.takeaway.security;

import com.pluto.takeaway.entity.UserAccount;
import com.pluto.takeaway.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUserDetailsService implements UserDetailsService {

	private final UserAccountRepository userAccountRepository;

	public AccountUserDetailsService(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		UserAccount userAccount = userAccountRepository.findByUsername(username)
			  .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

		return User.withUsername(userAccount.getUsername())
			  .password(userAccount.getPasswordHash())
			  .roles(userAccount.getRole())
			  .disabled(!userAccount.getEnabled())
			  .build();
	}
}
