package com.pluto.takeaway;

import com.pluto.takeaway.dto.*;
import com.pluto.takeaway.entity.Shop;
import com.pluto.takeaway.entity.UserAccount;
import com.pluto.takeaway.repository.ShopRepository;
import com.pluto.takeaway.repository.UserAccountRepository;
import com.pluto.takeaway.security.AccountUserDetailsService;
import com.pluto.takeaway.service.AdminMealService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AdminMealService adminMealService;

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
			  .andExpect(status().isNotFound())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
			  .andExpect(jsonPath("$.message").value("Resource not found"));
	}

	@Test
	void shouldRejectNonNumericShopId() throws Exception {
		mockMvc.perform(get("/api/shops/not-a-number/menu"))
			  .andExpect(status().isBadRequest())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			  .andExpect(jsonPath("$.message").value("Request input is invalid"));
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
			  .andExpect(status().isUnauthorized())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			  .andExpect(jsonPath("$.message").value("Authentication is required"));
	}

	@Test
	void shouldReturnCurrentUser() throws Exception {

		// Request for CSRF Token
		MockHttpSession authenticatedSession = logInAsAdminA();

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
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			  .andExpect(jsonPath("$.message").value("Invalid username or password"));

		// Get current user info without login
		mockMvc.perform(
				    get("/api/auth/me")
						.session(session)
			  )
			  .andExpect(status().isUnauthorized())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			  .andExpect(jsonPath("$.message").value("Authentication is required"));
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
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
			  .andExpect(jsonPath("$.message").value("CSRF token is missing or invalid"));

		// Get current user info without login
		mockMvc.perform(
				    get("/api/auth/me")
						.session(session)
			  )
			  .andExpect(status().isUnauthorized())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			  .andExpect(jsonPath("$.message").value("Authentication is required"));
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
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
			  .andExpect(jsonPath("$.message").value("CSRF token is missing or invalid"));

		// Get current user info
		mockMvc.perform(
				    get("/api/auth/me")
						.session(session)
			  )
			  .andExpect(status().isUnauthorized())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			  .andExpect(jsonPath("$.message").value("Authentication is required"));
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
			  .andExpect(status().isUnauthorized())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			  .andExpect(jsonPath("$.message").value("Authentication is required"));
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
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
			  .andExpect(jsonPath("$.message").value("CSRF token is missing or invalid"));

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
			  .andExpect(status().isUnauthorized())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			  .andExpect(jsonPath("$.message").value("Authentication is required"));
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
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
			  .andExpect(jsonPath("$.message").value("Access is denied"));
	}

	@Test
	void shouldAllowAdminToReadOwnShop() throws Exception {

		// Get CSRF
		MockHttpSession authenticatedSession = logInAsAdminA();

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
		MockHttpSession authenticatedSession = logInAsAdminA();

		// Access an admin shop that isn't owned by them
		mockMvc.perform(
				    get("/api/admin/shops/2")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
			  .andExpect(jsonPath("$.message").value("Access is denied"));
	}

	@Test
	void shouldAllowSecondAdminToReadOwnShop() throws Exception {

		// Get CSRF
		MockHttpSession authenticatedSession = logInAsAdminB();

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
		MockHttpSession authenticatedSession = logInAsAdminB();

		// Access an admin shop that isn't owned by them
		mockMvc.perform(
				    get("/api/admin/shops/1")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
			  .andExpect(jsonPath("$.message").value("Access is denied"));
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldAllowAdminToUpdateOwnShopName() throws Exception {

		MockHttpSession authenticatedSession = logInAsAdminA();

		// Get fresh CSRF Token
		MvcResult freshCsrfResult = mockMvc.perform(
				    get("/api/auth/csrf")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andReturn();

		CsrfResponse freshCsrfContent = getCsrfContent(freshCsrfResult);


		// Create DTO and convert to JSON
		String updateName = "Pluto Kitchen Update";
		UpdateShopRequest request = new UpdateShopRequest(updateName);
		String requestBody = jsonMapper.writeValueAsString(request);

		// Request to update shop name and verify response body
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(freshCsrfContent.headerName(), freshCsrfContent.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.id").value(1L))
			  .andExpect(jsonPath("$.name").value(updateName))
			  .andExpect(jsonPath("$.status").value("OPEN"));

		// Get updated shop from DB and verify changes
		Shop updatedShop = shopRepository.findById(1L).orElseThrow();

		assertThat(updatedShop.getName()).isEqualTo(updateName);
		assertThat(updatedShop.getStatus()).isEqualTo("OPEN");

		// Verify this update doesn't affect other rows
		Shop shop2 = shopRepository.findById(2L).orElseThrow();

		assertThat(shop2.getName()).isEqualTo("Harbour Cafe");
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRequireLoginToUpdateShop() throws Exception {

		MvcResult csrfResult = getCsrfResult();

		CsrfResponse csrfContent = getCsrfContent(csrfResult);

		MockHttpSession session = getMockHttpSession(csrfResult);

		// Request to update shop anonymously
		UpdateShopRequest request = new UpdateShopRequest("Should not update shop name");
		String requestJson = jsonMapper.writeValueAsString(request);

		// Should get 401
		// DB entries should be unchanged
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(session)
						.header(csrfContent.headerName(), csrfContent.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
			  )
			  .andExpect(status().isUnauthorized())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
			  .andExpect(jsonPath("$.message").value("Authentication is required"));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectCustomerShopUpdate() throws Exception {

		MvcResult csrfResult = getCsrfResult();

		CsrfResponse csrfContent = getCsrfContent(csrfResult);

		MockHttpSession session = getMockHttpSession(csrfResult);

		// Log in as a customer
		MockHttpSession authenticatedSession = logInCustomerAAndReturnAuthenticatedSession(session, csrfContent);

		// Get fresh CSRF Token
		MvcResult freshCsrfResult = mockMvc.perform(
				    get("/api/auth/csrf").session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andReturn();

		CsrfResponse freshCsrfContent = getCsrfContent(freshCsrfResult);

		// Request to update shop as a customer
		UpdateShopRequest request = new UpdateShopRequest("Should not update shop name");
		String requestJson = jsonMapper.writeValueAsString(request);

		// Should get 403
		// DB entries should be unchanged
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(freshCsrfContent.headerName(), freshCsrfContent.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
			  )
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
			  .andExpect(jsonPath("$.message").value("Access is denied"));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectUpdateToAnotherShop() throws Exception {

		MockHttpSession authenticatedSession = logInAsAdminB();

		// Get fresh CSRF Token
		MvcResult freshCsrfResult = mockMvc.perform(
				    get("/api/auth/csrf").session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andReturn();

		CsrfResponse freshCsrfContent = getCsrfContent(freshCsrfResult);

		// Request to update shop
		UpdateShopRequest request = new UpdateShopRequest("Should not update shop name");
		String requestJson = jsonMapper.writeValueAsString(request);

		// Should get 403
		// DB entries should be unchanged
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(freshCsrfContent.headerName(), freshCsrfContent.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
			  )
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
			  .andExpect(jsonPath("$.message").value("Access is denied"));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectShopUpdateWithoutCsrfToken() throws Exception {

		MockHttpSession authenticatedSession = logInAsAdminA();

		// Request to update shop without CSRF Token
		UpdateShopRequest request = new UpdateShopRequest("Should not update shop name");
		String requestJson = jsonMapper.writeValueAsString(request);

		// Should return 403
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
			  )
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
			  .andExpect(jsonPath("$.message").value("CSRF token is missing or invalid"));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectShopUpdateWithInvalidCsrfToken() throws Exception {

		MockHttpSession authenticatedSession = logInAsAdminA();

		// Get fresh CSRF Token
		MvcResult freshCsrfResult = mockMvc.perform(
				    get("/api/auth/csrf").session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andReturn();

		CsrfResponse freshCsrfContent = getCsrfContent(freshCsrfResult);

		// Request to update shop with invalid Token
		String invalidCsrfToken = "invalid-csrf-token";

		UpdateShopRequest request = new UpdateShopRequest("Should not update shop name");
		String requestJson = jsonMapper.writeValueAsString(request);

		// Should return 403
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(freshCsrfContent.headerName(), invalidCsrfToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestJson)
			  )
			  .andExpect(status().isForbidden())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
			  .andExpect(jsonPath("$.message").value("CSRF token is missing or invalid"));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectMissingShopName() throws Exception {

		MockHttpSession authenticatedSession = logInAsAdminA();

		// Get fresh CSRF
		CsrfResponse freshCsrf = getCsrf(authenticatedSession);

		// Request to update shop with missing shop name
		// Should return 400
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(freshCsrf.headerName(), freshCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}")
			  )
			  .andExpect(status().isBadRequest())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			  .andExpect(jsonPath("$.message").value("Request validation failed"));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectNullShopName() throws Exception {

		MockHttpSession authenticatedSession = logInAsAdminA();

		// Get fresh CSRF
		CsrfResponse freshCsrf = getCsrf(authenticatedSession);

		// Request to update shop with null shop name
		String requestBody = jsonMapper.writeValueAsString(new UpdateShopRequest(null));

		// Should return 400
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(freshCsrf.headerName(), freshCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
			  )
			  .andExpect(status().isBadRequest())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			  .andExpect(jsonPath("$.message").value("Request validation failed"));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectBlankShopName() throws Exception {

		MockHttpSession authenticatedSession = logInAsAdminA();

		// Get fresh CSRF
		CsrfResponse freshCsrf = getCsrf(authenticatedSession);

		// Request to update shop with null shop name
		String requestBody = jsonMapper.writeValueAsString(new UpdateShopRequest("      "));

		// Should return 400
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(freshCsrf.headerName(), freshCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
			  )
			  .andExpect(status().isBadRequest())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			  .andExpect(jsonPath("$.message").value("Request validation failed"));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectShopNameLongerThanLimit() throws Exception {

		MockHttpSession authenticatedSession = logInAsAdminA();

		// Get fresh CSRF
		CsrfResponse freshCsrf = getCsrf(authenticatedSession);

		// Request to update shop with name longer than limit
		String requestBody = jsonMapper.writeValueAsString(new UpdateShopRequest("a".repeat(101)));

		// Should return 400
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(freshCsrf.headerName(), freshCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
			  )
			  .andExpect(status().isBadRequest())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			  .andExpect(jsonPath("$.message").value("Request validation failed"))
			  .andExpect(result ->
				    assertThat(result.getResolvedException())
						.isInstanceOf(MethodArgumentNotValidException.class));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldAcceptShopNameAtMaximumLength() throws Exception {

		MockHttpSession authenticatedSession = logInAsAdminA();

		// Get fresh CSRF
		CsrfResponse freshCsrf = getCsrf(authenticatedSession);

		// Request to update shop with name at max length
		String requestBody = jsonMapper.writeValueAsString(new UpdateShopRequest("a".repeat(100)));

		// Should return 200
		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(freshCsrf.headerName(), freshCsrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.name").value("a".repeat(100)));
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectMalformedShopUpdateJson() throws Exception {
		MockHttpSession authenticatedSession = logInAsAdminA();
		CsrfResponse csrf = getCsrf(authenticatedSession);

		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(authenticatedSession)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":")
			  )
			  .andExpect(status().isBadRequest())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			  .andExpect(jsonPath("$.message").value("Request input is invalid"))
			  .andExpect(result ->
				    assertThat(result.getResolvedException())
						.isInstanceOf(HttpMessageNotReadableException.class));

		assertShopNamesUnchanged();
	}

	@Test
	@Sql("/test-data/reset-shop-names.sql")
	@Sql(
		  scripts = "/test-data/reset-shop-names.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldIgnoreUnknownFieldsWhenUpdatingShop() throws Exception {

		MockHttpSession session = logInAsAdminA();

		CsrfResponse csrf = getCsrf(session);

		String requestBody = """
			  {
			      "name": "Pluto Kitchen Renamed",
			      "id": 2,
			      "shopId": 2,
			      "status": "CLOSED"
			  }
			  """;

		mockMvc.perform(
				    patch("/api/admin/shops/1")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.id").value(1L))
			  .andExpect(jsonPath("$.name").value("Pluto Kitchen Renamed"))
			  .andExpect(jsonPath("$.status").value("OPEN"));

		Shop shop1 = shopRepository.findById(1L).orElseThrow();
		Shop shop2 = shopRepository.findById(2L).orElseThrow();

		assertThat(shop1.getName()).isEqualTo("Pluto Kitchen Renamed");
		assertThat(shop1.getStatus()).isEqualTo("OPEN");

		assertThat(shop2.getName()).isEqualTo("Harbour Cafe");
		assertThat(shop2.getStatus()).isEqualTo("OPEN");

		assertThat(
			  userAccountRepository.findByUsername("admin_a")
				    .orElseThrow()
				    .getShopId()
		).isEqualTo(1L);
	}

	@Test
	void shouldRotateSessionIdOnLogin() throws Exception {

		MvcResult csrfResult = getCsrfResult();
		CsrfResponse csrfContent = getCsrfContent(csrfResult);
		MockHttpSession session = getMockHttpSession(csrfResult);

		String sessionId = session.getId();

		MockHttpSession authenticatedSession = logInAdminAAndReturnAuthenticatedSession(session, csrfContent);

		assertThat(authenticatedSession.getId())
			  .isNotEqualTo(sessionId);

		mockMvc.perform(
				    get("/api/auth/me")
						.session(authenticatedSession)
			  )
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.username").value("admin_a"));
	}

	@Test
	@Sql(
		  statements = "UPDATE user_account SET enabled = FALSE WHERE username = 'customer_a'"
	)
	@Sql(
		  statements = "UPDATE user_account SET enabled = TRUE WHERE username = 'customer_a'",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectDisabledAccountLogin() throws Exception {
		MvcResult csrfResult = getCsrfResult();
		CsrfResponse csrfContent = getCsrfContent(csrfResult);
		MockHttpSession session = getMockHttpSession(csrfResult);

		// Log in as a disabled account
		mockMvc.perform(
				    post("/api/auth/login")
						.session(session)
						.header(csrfContent.headerName(), csrfContent.token())
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("username", "customer_a")
						.param("password", "password")
			  )
			  .andExpect(status().isUnauthorized())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

		// Get current user
		mockMvc.perform(
				    get("/api/auth/me")
						.session(session)
			  )
			  .andExpect(status().isUnauthorized())
			  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			  .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	@Sql(
		  scripts = "/test-data/cleanup-created-meal.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldCreateCompleteMealForOwnedShop() throws Exception {

		MockHttpSession session = logInAsAdminA();

		CsrfResponse csrf = getCsrf(session);
		MvcResult result = mockMvc.perform(
				    post("/api/admin/shops/1/meals")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							  {
							        "categoryId": 10,
							        "name": "Create Meal Integration Fixture",
							        "basePrice": 58.00,
							        "groups": [{
							              "name": "Choice",
							              "sortOrder": 10,
							              "options": [
							                    {
							                          "label": "No Drink",
							                          "supplement": 0.00,
							                          "sortOrder": 20
							                    },
							                    {
							                          "productId": 100,
							                          "supplement": 3.00,
							                          "sortOrder": 10
							                    }
							              ]
							        }]
							  }
							  """)
			  )
			  .andExpect(status().isCreated())
			  .andExpect(jsonPath("$.shopId").value(1))
			  .andExpect(jsonPath("$.categoryId").value(10))
			  .andExpect(jsonPath("$.status").value("UNAVAILABLE"))
			  .andExpect(jsonPath("$.groups[0].options.length()").value(2))
			  .andExpect(jsonPath("$.groups[0].options[0].name").value("Chicken Rice"))
			  .andExpect(jsonPath("$.groups[0].options[0].supplement").value(3.0))
			  .andExpect(jsonPath("$.groups[0].options[1].name").value("No Drink"))
			  .andExpect(jsonPath("$.groups[0].options[1].productId").value(nullValue()))
			  .andReturn();

		MealResponse response = jsonMapper.readValue(
			  result.getResponse().getContentAsString(),
			  MealResponse.class
		);

		Long mealId = response.id();

		MealGroupResponse group = response.groups().getFirst();

		Long savedShopId = jdbcTemplate.queryForObject(
			  "SELECT shop_id FROM meal where id = ?",
			  Long.class,
			  mealId
		);

		assertThat(savedShopId).isEqualTo(1L);

		Long savedMealId = jdbcTemplate.queryForObject(
			  "SELECT meal_id FROM meal_option_group WHERE id = ?",
			  Long.class,
			  group.id()
		);

		assertThat(savedMealId).isEqualTo(mealId);

		List<Long> savedOptionIds = jdbcTemplate.queryForList(
			  "SELECT id from meal_option WHERE group_id = ?",
			  Long.class,
			  group.id()
		);

		assertThat(savedOptionIds).containsExactlyInAnyOrder(
			  group.options().get(0).id(),
			  group.options().get(1).id()
		);
	}

	@Test
	void shouldRejectMealWithForeignShopProduct() throws Exception {

		MockHttpSession session = logInAsAdminA();

		CsrfResponse csrf = getCsrf(session);
		mockMvc.perform(
				    post("/api/admin/shops/1/meals")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							  {
							        "categoryId": 10,
							        "name": "Rejected Foreign Product Fixture",
							        "basePrice": 58.00,
							        "groups": [{
							              "name": "Choice",
							              "sortOrder": 10,
							              "options": [
							                    {
							                          "label": "No Drink",
							                          "supplement": 0.00,
							                          "sortOrder": 20
							                    },
							                    {
							                          "productId": 200,
							                          "supplement": 3.00,
							                          "sortOrder": 10
							                    }
							              ]
							        }]
							  }
							  """)
			  )
			  .andExpect(status().isForbidden())
			  .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		Long savedMeals = jdbcTemplate.queryForObject(
			  "SELECT COUNT(*) FROM meal WHERE shop_id = ? AND name = ?",
			  Long.class,
			  1L,
			  "Rejected Foreign Product Fixture"
		);

		assertThat(savedMeals).isZero();

	}

	@Test
	void shouldRejectMealWithMissingProduct() throws Exception {

		MockHttpSession session = logInAsAdminA();

		CsrfResponse csrf = getCsrf(session);
		mockMvc.perform(
				    post("/api/admin/shops/1/meals")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							  {
							        "categoryId": 10,
							        "name": "Rejected Missing Product Fixture",
							        "basePrice": 58.00,
							        "groups": [{
							              "name": "Choice",
							              "sortOrder": 10,
							              "options": [
							                    {
							                          "label": "No Drink",
							                          "supplement": 0.00,
							                          "sortOrder": 20
							                    },
							                    {
							                          "productId": 999999,
							                          "supplement": 3.00,
							                          "sortOrder": 10
							                    }
							              ]
							        }]
							  }
							  """)
			  )
			  .andExpect(status().isNotFound())
			  .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

		Long savedMeals = jdbcTemplate.queryForObject(
			  "SELECT COUNT(*) FROM meal WHERE shop_id = ? AND name = ?",
			  Long.class,
			  1L,
			  "Rejected Missing Product Fixture"
		);

		assertThat(savedMeals).isZero();
	}

	@Test
	void shouldRejectMealWithInvalidNestedOption() throws Exception {

		MockHttpSession session = logInAsAdminA();

		CsrfResponse csrf = getCsrf(session);
		mockMvc.perform(
				    post("/api/admin/shops/1/meals")
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
							  {
							        "categoryId": 10,
							        "name": "Rejected Invalid Option Fixture",
							        "basePrice": 58.00,
							        "groups": [{
							              "name": "Choice",
							              "sortOrder": 10,
							              "options": [
							                    {
							                          "label": "No Drink",
							                          "supplement": 0.00,
							                          "sortOrder": 20
							                    },
							                    {
							                          "productId": 100,
							                          "label": "Override",
							                          "supplement": 3.00,
							                          "sortOrder": 10
							                    }
							              ]
							        }]
							  }
							  """)
			  )
			  .andExpect(status().isBadRequest())
			  .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

		Long savedMeals = jdbcTemplate.queryForObject(
			  "SELECT COUNT(*) FROM meal WHERE shop_id = ? AND name = ?",
			  Long.class,
			  1L,
			  "Rejected Invalid Option Fixture"
		);

		assertThat(savedMeals).isZero();
	}

	@Test
	void shouldRollBackMealWhenSecondOptionInsertFails() {

		CreateMealRequest request = new CreateMealRequest(
			  10L,
			  "Meal Rollback Fixture",
			  new BigDecimal("58.00"),
			  List.of(new CreateMealGroupRequest(
				    "Main",
				    10,
				    List.of(
						new CreateMealOptionRequest(
							  100L, null, BigDecimal.ZERO, 10
						),
						new CreateMealOptionRequest(
							  100L, null, new BigDecimal("-1.00"), 20
						)
				    )
			  ))
		);

		String countsSql = """
			            SELECT 
			                  (SELECT COUNT(*) FROM meal WHERE shop_id = 1) AS meals,
			                  (SELECT COUNT(*) FROM meal_option_group WHERE shop_id = 1) AS groups_count,
			                  (SELECT COUNT(*) FROM meal_option WHERE shop_id = 1) AS options
			  """;

		var before = jdbcTemplate.queryForMap(countsSql);

		assertThatThrownBy(() ->
			  adminMealService.createMeal("admin_a", 1L, request)
		)
			  .hasRootCauseInstanceOf(SQLException.class)
			  .hasStackTraceContaining("chk_meal_option_supplement");

		assertThat(jdbcTemplate.queryForMap(countsSql)).isEqualTo(before);
	}

	@Test
	@Sql(
		  scripts = "/test-data/cleanup-publication-meals.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldRejectUnavailableProductEvenWhenAnotherOptionCoversTheGroup() throws Exception {

		// Create a meal directly with the Service
		MealResponse meal = adminMealService.createMeal("admin_b", 2L,
			  new CreateMealRequest(20L, "Unavailable Publication Fixture", BigDecimal.TEN,
				    List.of(new CreateMealGroupRequest("Drink", 10, List.of(
						new CreateMealOptionRequest(201L, null, BigDecimal.ZERO, 10),
						new CreateMealOptionRequest(null, "No Drink", BigDecimal.ZERO, 20)
				    )))
			  )
		);

		MealGroupResponse group = meal.groups().getFirst();

		Long optionIdBackedByUnavailableProduct = group.options().get(0).id();

		Long noDrinkOptionId = group.options().get(1).id();

		// Publish the meal with the No Drink option
		MockHttpSession session = logInAsAdminB();

		CsrfResponse csrf = getCsrf(session);

		mockMvc.perform(
				    put("/api/admin/shops/{shopId}/meals/{mealId}/publication", 2L, meal.id())
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(
							  new PublishMealRequest(List.of(noDrinkOptionId))
						))
			  )
			  .andExpect(status().isNoContent())
			  .andExpect(content().string(""));

		// Verify the publication will be rejected due to an unavailable product
		mockMvc.perform(
				    put("/api/admin/shops/{shopId}/meals/{mealId}/publication", 2L, meal.id())
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(
							  new PublishMealRequest(List.of(optionIdBackedByUnavailableProduct, noDrinkOptionId))
						))
			  )
			  .andExpect(status().isConflict())
			  .andExpect(jsonPath("$.code").value("MEAL_NOT_PUBLISHABLE"));


		// Verify the enabled meal stays intact
		assertThat(jdbcTemplate.queryForObject(
			  "SELECT status FROM meal WHERE id = ?", String.class, meal.id()
		)).isEqualTo("AVAILABLE");
		assertThat(jdbcTemplate.queryForList(
			  "SELECT id FROM meal_option WHERE group_id = ? AND status = 'AVAILABLE'",
			  Long.class, group.id()
		)).containsExactly(noDrinkOptionId);
	}

	@Test
	@Sql(
		  scripts = "/test-data/cleanup-publication-meals.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldReplaceEnabledOptionsWhenRepublishing() throws Exception {

		// Create a meal directly with the Service
		MealResponse meal = adminMealService.createMeal("admin_b", 2L,
			  new CreateMealRequest(20L, "Replacement Publication Fixture", BigDecimal.TEN,
				    List.of(new CreateMealGroupRequest("Drink", 10, List.of(
						new CreateMealOptionRequest(200L, null, BigDecimal.ZERO, 10),
						new CreateMealOptionRequest(null, "No Drink", BigDecimal.ZERO, 20)
				    )))
			  )
		);

		MealGroupResponse group = meal.groups().getFirst();

		Long coffeeOptionId = group.options().getFirst().id();
		Long noDrinkOptionId = group.options().get(1).id();

		MockHttpSession session = logInAsAdminB();

		CsrfResponse csrf = getCsrf(session);

		// Enable only the No Drink option
		mockMvc.perform(
				    put("/api/admin/shops/{shopId}/meals/{mealId}/publication", 2L, meal.id())
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(
							  new PublishMealRequest(List.of(noDrinkOptionId))
						))
			  )
			  .andExpect(status().isNoContent())
			  .andExpect(content().string(""));

		// Should only have No Drink enabled
		assertThat(jdbcTemplate.queryForList(
			  "SELECT id FROM meal_option WHERE group_id = ? AND STATUS = 'AVAILABLE'",
			  Long.class,
			  group.id()
		)).containsExactly(noDrinkOptionId);

		// Enable only the coffee option
		mockMvc.perform(
				    put("/api/admin/shops/{shopId}/meals/{mealId}/publication", 2L, meal.id())
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(
							  new PublishMealRequest(List.of(coffeeOptionId))
						))
			  )
			  .andExpect(status().isNoContent())
			  .andExpect(content().string(""));

		// Should only have coffee enabled
		assertThat(jdbcTemplate.queryForList(
			  "SELECT id FROM meal_option WHERE group_id = ? AND STATUS = 'AVAILABLE'",
			  Long.class,
			  group.id()
		)).containsExactly(coffeeOptionId);
	}

	@Test
	@Sql(
		  scripts = "/test-data/cleanup-public-menu-meal.sql",
		  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
	)
	void shouldReflectProductAvailabilityInPublicMealMenu() throws Exception {

		// Create a meal directly with the Service
		MealResponse meal = adminMealService.createMeal("admin_b", 2L,
			  new CreateMealRequest(20L, "Public Meal Availability Fixture", BigDecimal.TEN,
				    List.of(new CreateMealGroupRequest("Drink", 10, List.of(
						new CreateMealOptionRequest(200L, null, BigDecimal.ZERO, 10),
						new CreateMealOptionRequest(null, "No Drink", BigDecimal.ZERO, 20)
				    )))
			  )
		);

		Long coffeeOptionId = meal.groups().getFirst().options().getFirst().id();

		// Try to read the menu
		// Should get nothing as the meal haven't been published
		mockMvc.perform(get("/api/shops/2/menu"))
			  .andExpect(status().isOk())
			  .andExpect(jsonPath("$.categories[0].meals").isEmpty());

		MockHttpSession session = logInAsAdminB();

		CsrfResponse csrf = getCsrf(session);

		// Publish the meal and enable only the Coffee option
		mockMvc.perform(
				    put("/api/admin/shops/{shopId}/meals/{mealId}/publication", 2L, meal.id())
						.session(session)
						.header(csrf.headerName(), csrf.token())
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonMapper.writeValueAsString(
							  new PublishMealRequest(List.of(coffeeOptionId))
						))
			  )
			  .andExpect(status().isNoContent())
			  .andExpect(content().string(""));

		String mealPath = "$.categories[0].meals[0]";
		String optionPath = mealPath + ".groups[0].options";

		// Read the menu
		mockMvc.perform(get("/api/shops/2/menu"))
			  .andExpect(status().isOk())
			  .andExpect(jsonPath(mealPath + ".name").value("Public Meal Availability Fixture"))
			  .andExpect(jsonPath(mealPath + ".configurable").value(true))
			  .andExpect(jsonPath(optionPath + ".length()").value(1))
			  .andExpect(jsonPath(optionPath + "[0].name").value("Coffee"))
			  .andExpect(jsonPath(optionPath + "[0].selectable").value(true));

		// Disable the option directly for simplicity
		// And verify 1 row will be affected
		assertThat(jdbcTemplate.update(
			  "UPDATE product SET status = ? WHERE shop_id = ? AND id = ?",
			  "UNAVAILABLE", 2L, 200L
		)).isEqualTo(1);

		// Read the menu again
		// And the coffee option should be unselectable now
		mockMvc.perform(get("/api/shops/2/menu"))
			  .andExpect(status().isOk())
			  .andExpect(jsonPath(mealPath + ".name").value("Public Meal Availability Fixture"))
			  .andExpect(jsonPath(mealPath + ".configurable").value(false))
			  .andExpect(jsonPath(optionPath + ".length()").value(1))
			  .andExpect(jsonPath(optionPath + "[0].name").value("Coffee"))
			  .andExpect(jsonPath(optionPath + "[0].selectable").value(false));
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

	private void assertShopNamesUnchanged() {

		assertThat(shopRepository.findById(1L).orElseThrow().getName())
			  .isEqualTo("Pluto Kitchen");

		assertThat(shopRepository.findById(2L).orElseThrow().getName())
			  .isEqualTo("Harbour Cafe");
	}

	private MockHttpSession logInAsAdminA() throws Exception {
		MvcResult csrfResult = getCsrfResult();

		CsrfResponse csrfContent = getCsrfContent(csrfResult);

		MockHttpSession session = getMockHttpSession(csrfResult);

		return logInAdminAAndReturnAuthenticatedSession(session, csrfContent);
	}

	private CsrfResponse getCsrf(MockHttpSession session) throws Exception {
		MvcResult freshCsrfResult = mockMvc.perform(
				    get("/api/auth/csrf").session(session)
			  )
			  .andExpect(status().isOk())
			  .andReturn();
		return getCsrfContent(freshCsrfResult);
	}

	private MockHttpSession logInAsAdminB() throws Exception {

		MvcResult csrfResult = getCsrfResult();

		CsrfResponse csrfContent = getCsrfContent(csrfResult);

		MockHttpSession session = getMockHttpSession(csrfResult);

		return logInAdminBAndReturnSession(session, csrfContent);
	}

}
