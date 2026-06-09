package com.haufe.beercatalogue.beer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haufe.beercatalogue.beer.dto.BeerRequest;
import com.haufe.beercatalogue.manufacturer.Manufacturer;
import com.haufe.beercatalogue.manufacturer.ManufacturerRepository;
import com.haufe.beercatalogue.security.AppUser;
import com.haufe.beercatalogue.security.AppUserRepository;
import com.haufe.beercatalogue.security.UserRole;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class BeerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    ManufacturerRepository manufacturerRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    Manufacturer m1;
    Manufacturer m2;
    Beer beer1;

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
        appUserRepository.deleteAll();
        manufacturerRepository.deleteAll();

        m1 = manufacturerRepository.save(new Manufacturer("BrewCo", "Germany"));
        m2 = manufacturerRepository.save(new Manufacturer("HopFarm", "Belgium"));

        appUserRepository.save(new AppUser("admin", passwordEncoder.encode("pass"), UserRole.ADMIN, null));
        appUserRepository.save(
            new AppUser("ownerA", passwordEncoder.encode("pass"), UserRole.MANUFACTURER, m1.getId()));
        appUserRepository.save(
            new AppUser("ownerB", passwordEncoder.encode("pass"), UserRole.MANUFACTURER, m2.getId()));

        beer1 = beerRepository.save(new Beer("Lager", new BigDecimal("5.0"), BeerType.LAGER, null, m1));
    }

    @Test
    void anonymous_GET_returns200() throws Exception {
        mockMvc.perform(get("/api/beers"))
            .andExpect(status().isOk());
    }

    @Test
    void anonymous_POST_returns401() throws Exception {
        mockMvc.perform(post("/api/beers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new BeerRequest("X", new BigDecimal("5.0"), BeerType.LAGER, null, m1.getId()))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void anonymous_PUT_returns401() throws Exception {
        mockMvc.perform(put("/api/beers/{id}", beer1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new BeerRequest("X", new BigDecimal("5.0"), BeerType.LAGER, null, m1.getId()))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void anonymous_DELETE_returns401() throws Exception {
        mockMvc.perform(delete("/api/beers/{id}", beer1.getId()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void ownerA_create_for_own_manufacturer_returns201() throws Exception {
        mockMvc.perform(post("/api/beers")
                .with(httpBasic("ownerA", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new BeerRequest("IPA", new BigDecimal("6.0"), BeerType.IPA, null, m1.getId()))))
            .andExpect(status().isCreated());
    }

    @Test
    void ownerA_create_for_other_manufacturer_returns403() throws Exception {
        mockMvc.perform(post("/api/beers")
                .with(httpBasic("ownerA", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new BeerRequest("Stout", new BigDecimal("4.5"), BeerType.STOUT, null, m2.getId()))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void ownerA_update_own_beer_returns200() throws Exception {
        mockMvc.perform(put("/api/beers/{id}", beer1.getId())
                .with(httpBasic("ownerA", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new BeerRequest("Updated Lager", new BigDecimal("5.0"), BeerType.LAGER, null, m1.getId()))))
            .andExpect(status().isOk());
    }

    @Test
    void ownerB_update_ownerA_beer_returns403() throws Exception {
        mockMvc.perform(put("/api/beers/{id}", beer1.getId())
                .with(httpBasic("ownerB", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new BeerRequest("Hijacked", new BigDecimal("5.0"), BeerType.LAGER, null, m1.getId()))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void ownerA_delete_own_beer_returns204() throws Exception {
        mockMvc.perform(delete("/api/beers/{id}", beer1.getId())
                .with(httpBasic("ownerA", "pass")))
            .andExpect(status().isNoContent());
    }

    @Test
    void ownerB_delete_ownerA_beer_returns403() throws Exception {
        mockMvc.perform(delete("/api/beers/{id}", beer1.getId())
                .with(httpBasic("ownerB", "pass")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void admin_create_for_any_manufacturer_returns201() throws Exception {
        mockMvc.perform(post("/api/beers")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new BeerRequest("Admin Beer", new BigDecimal("5.5"), BeerType.ALE, null, m2.getId()))))
            .andExpect(status().isCreated());
    }

    @Test
    void admin_update_any_beer_returns200() throws Exception {
        mockMvc.perform(put("/api/beers/{id}", beer1.getId())
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new BeerRequest("Admin Updated", new BigDecimal("5.0"), BeerType.LAGER, null, m1.getId()))))
            .andExpect(status().isOk());
    }

    @Test
    void admin_delete_any_beer_returns204() throws Exception {
        mockMvc.perform(delete("/api/beers/{id}", beer1.getId())
                .with(httpBasic("admin", "pass")))
            .andExpect(status().isNoContent());
    }
}
