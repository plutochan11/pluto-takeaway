package com.pluto.takeaway;

import com.pluto.takeaway.entity.UserAccount;
import com.pluto.takeaway.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseConfiguration.class)
class PlutoTakeawayApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

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
		String expectRole =  "CUSTOMER";
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
	void contextLoads() {
	}

}
