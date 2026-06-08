package com.haufe.beercatalogue.manufacturer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haufe.beercatalogue.beer.BeerRepository;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerRequest;
import com.haufe.beercatalogue.security.AppUser;
import com.haufe.beercatalogue.security.AppUserRepository;
import com.haufe.beercatalogue.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class ManufacturerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    ManufacturerRepository repository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    Manufacturer m1;
    Manufacturer m2;
    Manufacturer m3;

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
        appUserRepository.deleteAll();
        repository.deleteAll();

        m1 = repository.save(new Manufacturer("BrewCo", "Germany"));
        m2 = repository.save(new Manufacturer("HopFarm", "Belgium"));
        m3 = repository.save(new Manufacturer("FreeBrew", "Austria"));

        appUserRepository.save(new AppUser("admin", passwordEncoder.encode("pass"), UserRole.ADMIN, null));
        appUserRepository.save(
            new AppUser("ownerA", passwordEncoder.encode("pass"), UserRole.MANUFACTURER, m1.getId()));
        appUserRepository.save(
            new AppUser("ownerB", passwordEncoder.encode("pass"), UserRole.MANUFACTURER, m2.getId()));
    }

    @Test
    void anonymous_GET_returns200() throws Exception {
        mockMvc.perform(get("/api/manufacturers"))
            .andExpect(status().isOk());
    }

    @Test
    void anonymous_POST_returns401() throws Exception {
        mockMvc.perform(post("/api/manufacturers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("New", "Country"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_PUT_returns401() throws Exception {
        mockMvc.perform(put("/api/manufacturers/{id}", m1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("Updated", "Country"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymous_DELETE_returns401() throws Exception {
        mockMvc.perform(delete("/api/manufacturers/{id}", m1.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void manufacturer_role_POST_returns403() throws Exception {
        mockMvc.perform(post("/api/manufacturers")
                .with(httpBasic("ownerA", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("New", "Country"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void ownerA_update_own_manufacturer_returns200() throws Exception {
        mockMvc.perform(put("/api/manufacturers/{id}", m1.getId())
                .with(httpBasic("ownerA", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("Updated BrewCo", "Germany"))))
            .andExpect(status().isOk());
    }

    @Test
    void ownerB_update_ownerA_manufacturer_returns403() throws Exception {
        mockMvc.perform(put("/api/manufacturers/{id}", m1.getId())
                .with(httpBasic("ownerB", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("Hijacked", "Germany"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void manufacturer_role_DELETE_returns403() throws Exception {
        mockMvc.perform(delete("/api/manufacturers/{id}", m1.getId())
                .with(httpBasic("ownerA", "pass")))
            .andExpect(status().isForbidden());
    }

    @Test
    void admin_POST_returns201() throws Exception {
        mockMvc.perform(post("/api/manufacturers")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("New Brewery", "France"))))
            .andExpect(status().isCreated());
    }

    @Test
    void admin_update_any_manufacturer_returns200() throws Exception {
        mockMvc.perform(put("/api/manufacturers/{id}", m1.getId())
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("Admin Updated", "Germany"))))
            .andExpect(status().isOk());
    }

    @Test
    void admin_DELETE_returns204() throws Exception {
        mockMvc.perform(delete("/api/manufacturers/{id}", m3.getId())
                .with(httpBasic("admin", "pass")))
            .andExpect(status().isNoContent());
    }
}
