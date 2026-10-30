package com.pluto.takeaway;

import com.pluto.takeaway.entity.UserAccount;
import com.pluto.takeaway.repository.UserAccountRepository;
import com.pluto.takeaway.security.AccountUserDetailsService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.pluto.takeaway.dto.CsrfResponse;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseConfiguration.class)
class PlutoTakeawayApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private AccountUserDetailsService accountUserDetailsService;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void shouldReturnShopOneMenu() throws Exception {
		mockMvc.perform(get("/api/shops/1/menu"))
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.shop.id").value(1))
			  .andExpect(jsonPath("$.categories.length()").value(1))
			  .andExpect(
				    jsonPath("$.categories[0].products.length()").value(1)
			  )
			  .andExpect(
				    jsonPath("$.categories[0].products[0].name")
						.value("Chicken Rice")
			  );
	}

	@Test
	void shouldReturnShopByIdOrder() throws Exception {

		mockMvc.perform(get("/api/shops"))
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.length()").value(4))
			  .andExpect(jsonPath("$[0].id").value(1))
			  .andExpect(jsonPath("$[1].id").value(2))
			  .andExpect(jsonPath("$[2].id").value(9001))
			  .andExpect(jsonPath("$[3].id").value(9002));
	}

	@Test
	void shouldReturnShopTwoMenu() throws Exception {

		mockMvc.perform(get("/api/shops/2/menu"))
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.shop.id").value(2))
			  .andExpect(jsonPath("$.categories.length()").value(1))
			  .andExpect(jsonPath("$.categories[0].products.length()").value(2))
			  .andExpect(jsonPath("$.categories[0].products[0].name").value("Coffee"))
			  .andExpect(jsonPath("$.categories[0].products[1].name").value("Lemon Tea"))
			  .andExpect(jsonPath("$.categories[0].products[1].status").value("UNAVAILABLE"));
	}

	@Test
	void shouldReturnsEmptyMenuForExistingShop() throws Exception {
		mockMvc.perform(get("/api/shops/9001/menu"))
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.shop.id").value(9001))
			  .andExpect(jsonPath("$.categories.length()").value(0));
	}

	@Test
	void shouldOrderCategoriesAndKeepEmptyOnes() throws Exception {
		mockMvc.perform(get("/api/shops/9002/menu"))
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.shop.id").value(9002))
			  .andExpect(jsonPath("$.categories.length()").value(3))
			  .andExpect(jsonPath("$.categories[0].id").value(9102))
			  .andExpect(jsonPath("$.categories[1].id").value(9103))
			  .andExpect(jsonPath("$.categories[2].id").value(9101))
			  .andExpect(jsonPath("$.categories[0].products.length()").value(1))
			  .andExpect(jsonPath("$.categories[1].products.length()").value(0))
			  .andExpect(jsonPath("$.categories[2].products.length()").value(0))
			  .andExpect(
				    jsonPath("$.categories[0].products[0].name")
						.value("Test Coffee")
			  );
	}

	@Test
	void shouldReturnNotFoundForUnknownShop() throws Exception {
		mockMvc.perform(get("/api/shops/999999/menu"))
			  .andExpect(status().isNotFound());
	}

	@Test
	void shouldRejectNonNumericShopId() throws Exception {
		mockMvc.perform(get("/api/shops/not-a-number/menu"))
			  .andExpect(status().isBadRequest());
	}

	@Test
	void shouldLoadShopAdminAccount() throws Exception {

		String username = "admin_a";
		String expectPassword = "password";
		String wrongPassword = "wrong_password";
		String expectRole = "SHOP_ADMIN";
		Boolean expectEnabled = true;
		Long expectShopId = 1L;
		PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

		UserAccount userAccount = userAccountRepository.findByUsername(username)
			  .orElseThrow();

		assertThat(userAccount.getRole()).isEqualTo(expectRole);
		assertThat(userAccount.getEnabled()).isEqualTo(expectEnabled);
		assertThat(userAccount.getShopId()).isEqualTo(expectShopId);
		assertThat(passwordEncoder.matches(expectPassword, userAccount.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(wrongPassword, userAccount.getPasswordHash())).isFalse();
	}

	@Test
	void shouldLoadCustomerWithoutShop() throws Exception {

		String username = "customer_a";
		String expectRole = "CUSTOMER";
		Long expectShopId = null;

		UserAccount returnedUserAccount = userAccountRepository.findByUsername(username)
			  .orElseThrow();

		assertThat(returnedUserAccount.getRole()).isEqualTo(expectRole);
		assertThat(returnedUserAccount.getShopId()).isEqualTo(expectShopId);
	}

	@Test
	void shouldReturnEmptyForUnknownUsername() throws Exception {

		String unknownUsername = "unknown_username";

		assertThat(userAccountRepository
			  .findByUsername(unknownUsername)
			  .isEmpty()
		).isTrue();
	}

	@Test
	void shouldLoadAdminAsUserDetails() {

		String username = "admin_a";
		UserDetails userDetails = accountUserDetailsService.loadUserByUsername(username);

		assertThat(userDetails.getUsername())
			  .isEqualTo(username);
		assertThat(userDetails.isEnabled())
			  .isTrue();
		assertThat(userDetails.getAuthorities())
			  .extracting(GrantedAuthority::getAuthority)
			  .containsExactly("ROLE_SHOP_ADMIN");
	}

	@Test
	void shouldThrowForUnknownSecurityUser() {

		String unknownUsername = "unknown_username";

		assertThatThrownBy(() ->
			  accountUserDetailsService.loadUserByUsername(unknownUsername)
		).isInstanceOf(UsernameNotFoundException.class);
	}

	@Test
	void shouldExposeCsrfToken() throws Exception {

		mockMvc.perform(get("/api/auth/csrf"))
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.headerName").isNotEmpty())
			  .andExpect(jsonPath("$.parameterName").isNotEmpty())
			  .andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void shouldRequireAuthenticationForCurrentUser() throws Exception {

		mockMvc.perform(get("/api/auth/me"))
			  .andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnCurrentUser() throws Exception {

		// Request for CSRF Token
		MvcResult csrfResult = getCsrfResult();

		CsrfResponse csrf = getCsrfContent(csrfResult);

		MockHttpSession session = getMockHttpSession(csrfResult);

		// Log in admin_a with CSRF Token
		MockHttpSession authenticatedSession = logInAdminAAndReturnAuthenticatedSession(session, csrf);

		String username = "admin_a";
		String password = "password";
		String role = "SHOP_ADMIN";
		mockMvc.perform(
				    get("/api/auth/me")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.username").value(username))
			  .andExpect(jsonPath("$.role").value(role))
			  .andExpect(jsonPath("$.shopId").value(1L))
			  .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath());
	}

	@Test
	void shouldRejectWrongPassword() throws Exception {

		// Request for CSRF Token
		MvcResult csrfResult = getCsrfResult();

		CsrfResponse csrf = getCsrfContent(csrfResult);

		MockHttpSession session = getMockHttpSession(csrfResult);

		// Log in with wrong password
		mockMvc.perform(
				    post("/api/auth/login")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "admin_a")
						.param("password", "wrong_password")
			  )
			  .andExpect(status().isUnauthorized())
			  .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

		// Get current user info without login
		mockMvc.perform(
				    get("/api/auth/me")
						.session(session)
			  )
			  .andExpect(status().isUnauthorized());
	}

	@Test
	void shouldRejectLoginWithoutCsrfToken() throws Exception {

		// Request for CSRF Token
		MvcResult csrfResult = getCsrfResult();

		// Get session
		MockHttpSession session = getMockHttpSession(csrfResult);

		// Log in without CSRF Token
		mockMvc.perform(
				    post("/api/auth/login")
						.session(session)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "admin_a")
						.param("password", "password")
			  )
			  .andExpect(status().isForbidden());

		// Get current user info without login
		mockMvc.perform(
				    get("/api/auth/me")
						.session(session)
			  )
			  .andExpect(status().isUnauthorized());
	}

	@Test
	void shouldRejectLoginWithInvalidCsrfToken() throws Exception {

		// Request for CSRF Token
		MvcResult csrfResult = getCsrfResult();

		// Get CSRF info
		CsrfResponse csrf = getCsrfContent(csrfResult);

		// Get session
		MockHttpSession session = getMockHttpSession(csrfResult);

		// Log in with wrong CSRF Token
		mockMvc.perform(
				    post("/api/auth/login")
						.session(session)
						.header(csrf.headerName(), "wrong-csrf-token")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "admin_a")
						.param("password", "password")
			  )
			  .andExpect(status().isForbidden());

		// Get current user info
		mockMvc.perform(
				    get("/api/auth/me")
						.session(session)
			  )
			  .andExpect(status().isUnauthorized());
	}

	@Test
	void shouldLoginCustomerWithoutShop() throws Exception {

		// Request for CSRF Token
		MvcResult csrfResult = getCsrfResult();

		// Get CSRF info
		CsrfResponse csrf = getCsrfContent(csrfResult);

		// Get session info
		MockHttpSession session = getMockHttpSession(csrfResult);

		// Log in customer_a with CSRF Token
		MockHttpSession authenticatedSession = logInCustomerAAndReturnAuthenticatedSession(session, csrf);

		mockMvc.perform(
				    get("/api/auth/me")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.username").value("customer_a"))
			  .andExpect(jsonPath("$.role").value("CUSTOMER"))
			  .andExpect(jsonPath("$.shopId").value(nullValue()))
			  .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath());
	}

	@Test
	void shouldLogoutAndInvalidateSession() throws Exception {

		// Get CSRF info
		MvcResult csrfResult = getCsrfResult();

		CsrfResponse csrf = getCsrfContent(csrfResult);

		MockHttpSession session = getMockHttpSession(csrfResult);

		// Log in with CSRF Token
		String username = "admin_a";
		String password = "password";
		String role = "SHOP_ADMIN";

		MvcResult loginResult = mockMvc.perform(
				    post("/api/auth/login")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", username)
						.param("password", password)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.username").value(username))
			  .andExpect(jsonPath("$.role").value(role))
			  .andExpect(jsonPath("$.shopId").value(1L))
			  .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath())
			  .andReturn();

		MockHttpSession authenticatedSession = getMockHttpSession(loginResult);

		// Get fresh CSRF Token
		MvcResult csrfAfterLogin = mockMvc.perform(
				    get("/api/auth/csrf")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andReturn();

		CsrfResponse logoutCsrf = jsonMapper.readValue(
			  csrfAfterLogin.getResponse().getContentAsString(),
			  CsrfResponse.class
		);

		mockMvc.perform(
				    post("/api/auth/logout")
						.session(authenticatedSession)
						.header(logoutCsrf.headerName(), logoutCsrf.token())
			  )
			  .andExpect(status().isNoContent());

		assertThat(authenticatedSession.isInvalid()).isTrue();

		mockMvc.perform(get("/api/auth/me"))
			  .andExpect(status().isUnauthorized());
	}

	@Test
	void shouldRejectLogoutWithoutCsrfToken() throws Exception {

		// Get CSRF info
		MvcResult csrfResult = getCsrfResult();

		CsrfResponse csrf = getCsrfContent(csrfResult);

		MockHttpSession session = getMockHttpSession(csrfResult);

		// Log in with CSRF Token
		String username = "admin_a";
		String password = "password";
		String role = "SHOP_ADMIN";

		MvcResult loginResult = mockMvc.perform(
				    post("/api/auth/login")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", username)
						.param("password", password)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.username").value(username))
			  .andExpect(jsonPath("$.role").value(role))
			  .andExpect(jsonPath("$.shopId").value(1L))
			  .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath())
			  .andReturn();

		MockHttpSession authenticatedSession = getMockHttpSession(loginResult);

		// Logout without CSRF Token
		mockMvc.perform(
				    post("/api/auth/logout")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isForbidden());

		assertThat(authenticatedSession.isInvalid()).isFalse();

		// Should get current user info successfully
		mockMvc.perform(
				    get("/api/auth/me")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.username").value(username))
			  .andExpect(jsonPath("$.role").value(role))
			  .andExpect(jsonPath("$.shopId").value(1L))
			  .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath());
	}

	@Test
	void shouldRequireLoginForAdminShop() throws Exception {

		// Access admin shop without login
		mockMvc.perform(get("/api/admin/shops/1"))
			  .andExpect(status().isUnauthorized());
	}

	@Test
	void shouldRejectCustomerAccessToAdminShop() throws Exception {

		// Get CSRF
		MvcResult csrfResult = getCsrfResult();
		CsrfResponse csrfContent = getCsrfContent(csrfResult);

		// Get Session
		MockHttpSession mockHttpSession = getMockHttpSession(csrfResult);

		// Log in as customer_a
		MockHttpSession authenticatedSession = logInCustomerAAndReturnAuthenticatedSession(mockHttpSession, csrfContent);

		// Access an admin shop
		mockMvc.perform(
				    get("/api/admin/shops/1")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isForbidden());
	}

	@Test
	void shouldAllowAdminToReadOwnShop() throws Exception {

		// Get CSRF
		MvcResult csrfResult = getCsrfResult();
		CsrfResponse csrfContent = getCsrfContent(csrfResult);

		// Get Session
		MockHttpSession mockHttpSession = getMockHttpSession(csrfResult);

		// Log in as admin_a
		MockHttpSession authenticatedSession = logInAdminAAndReturnAuthenticatedSession(mockHttpSession, csrfContent);

		// Access their own admin shop
		mockMvc.perform(
				    get("/api/admin/shops/1")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.id").value(1L))
			  .andExpect(jsonPath("$.name").value("Pluto Kitchen"))
			  .andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	void shouldRejectAdminAAccessToAnotherShop() throws Exception {

		// Get CSRF
		MvcResult csrfResult = getCsrfResult();
		CsrfResponse csrfContent = getCsrfContent(csrfResult);

		// Get Session
		MockHttpSession mockHttpSession = getMockHttpSession(csrfResult);

		// Log in as admin_a
		MockHttpSession authenticatedSession = logInAdminAAndReturnAuthenticatedSession(mockHttpSession, csrfContent);

		// Access an admin shop that isn't owned by them
		mockMvc.perform(
				    get("/api/admin/shops/2")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isForbidden());
	}

	@Test
	void shouldAllowSecondAdminToReadOwnShop() throws Exception {

		// Get CSRF
		MvcResult csrfResult = getCsrfResult();
		CsrfResponse csrfContent = getCsrfContent(csrfResult);

		// Get Session
		MockHttpSession mockHttpSession = getMockHttpSession(csrfResult);

		// Log in as admin_b
		MockHttpSession authenticatedSession = logInAdminBAndReturnSession(mockHttpSession, csrfContent);

		// Access their own admin shop
		mockMvc.perform(
				    get("/api/admin/shops/2")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.id").value(2L))
			  .andExpect(jsonPath("$.name").value("Harbour Cafe"))
			  .andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	void shouldRejectAdminBAccessToAnotherShop() throws Exception {

		// Get CSRF
		MvcResult csrfResult = getCsrfResult();
		CsrfResponse csrfContent = getCsrfContent(csrfResult);

		// Get Session
		MockHttpSession mockHttpSession = getMockHttpSession(csrfResult);

		// Log in as admin_b
		MockHttpSession authenticatedSession = logInAdminBAndReturnSession(mockHttpSession, csrfContent);

		// Access an admin shop that isn't owned by them
		mockMvc.perform(
				    get("/api/admin/shops/1")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isForbidden());
	}

	@Test
	void contextLoads() {
	}

	private @NonNull MvcResult getCsrfResult() throws Exception {
		return mockMvc.perform(get("/api/auth/csrf"))
			  .andExpect(status().isOk())
			  .andReturn();
	}

	private static @NonNull MockHttpSession getMockHttpSession(MvcResult csrfResult) {
		MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);

		assertThat(session).isNotNull();
		return session;
	}

	private CsrfResponse getCsrfContent(MvcResult csrfResult) throws UnsupportedEncodingException {
		return jsonMapper.readValue(
			  csrfResult.getResponse().getContentAsString(),
			  CsrfResponse.class
		);
	}

	private @NonNull MockHttpSession logInAdminAAndReturnAuthenticatedSession(MockHttpSession session, CsrfResponse csrf) throws Exception {

		MvcResult loginResult = mockMvc.perform(
				    post("/api/auth/login")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "admin_a")
						.param("password", "password")
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.username").value("admin_a"))
			  .andExpect(jsonPath("$.role").value("SHOP_ADMIN"))
			  .andExpect(jsonPath("$.shopId").value(1L))
			  .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath())
			  .andReturn();

		return getMockHttpSession(loginResult);
	}

	private @NonNull MockHttpSession logInCustomerAAndReturnAuthenticatedSession(MockHttpSession session, CsrfResponse csrf) throws Exception {
		MvcResult loginResult = mockMvc.perform(
				    post("/api/auth/login")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "customer_a")
						.param("password", "password")
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.username").value("customer_a"))
			  .andExpect(jsonPath("$.role").value("CUSTOMER"))
			  .andExpect(jsonPath("$.shopId").value(nullValue()))
			  .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath())
			  .andReturn();

		return getMockHttpSession(loginResult);
	}

	private @NonNull MockHttpSession logInAdminBAndReturnSession(MockHttpSession mockHttpSession, CsrfResponse csrfContent) throws Exception {
		MvcResult loginResult = mockMvc.perform(
				    post("/api/auth/login")
						.session(mockHttpSession)
						.header(csrfContent.headerName(), csrfContent.token())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "admin_b")
						.param("password", "password")
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.username").value("admin_b"))
			  .andExpect(jsonPath("$.role").value("SHOP_ADMIN"))
			  .andExpect(jsonPath("$.shopId").value(2L))
			  .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath())
			  .andReturn();

		MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
		assertThat(authenticatedSession).isNotNull();
		return authenticatedSession;
	}

}
