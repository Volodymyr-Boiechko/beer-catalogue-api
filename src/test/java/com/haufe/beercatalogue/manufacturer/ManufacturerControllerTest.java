package com.haufe.beercatalogue.manufacturer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class ManufacturerControllerTest {

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

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
        appUserRepository.deleteAll();
        repository.deleteAll();
        appUserRepository.save(new AppUser("admin", passwordEncoder.encode("pass"), UserRole.ADMIN, null));
    }

    @Test
    void list_emptyDatabase_returns200AndEmptyPage() throws Exception {
        mockMvc.perform(get("/api/manufacturers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void list_manyManufacturers_paginatesAndSorts() throws Exception {
        repository.save(new Manufacturer("Zywiec", "Poland"));
        repository.save(new Manufacturer("Asahi", "Japan"));
        repository.save(new Manufacturer("Carlsberg", "Denmark"));
        repository.save(new Manufacturer("Budweiser", "USA"));
        repository.save(new Manufacturer("Heineken", "Netherlands"));

        mockMvc.perform(get("/api/manufacturers?page=0&size=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].name").value("Asahi"))
            .andExpect(jsonPath("$.content[1].name").value("Budweiser"))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/manufacturers?page=2&size=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Zywiec"))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void search_byName_caseInsensitive_returnsMatchingManufacturers() throws Exception {
        repository.save(new Manufacturer("Heineken", "Netherlands"));
        repository.save(new Manufacturer("Guinness", "Ireland"));
        repository.save(new Manufacturer("Asahi", "Japan"));

        mockMvc.perform(get("/api/manufacturers").param("name", "eken"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Heineken"));
    }

    @Test
    void search_byCountry_caseInsensitive_returnsMatchingManufacturers() throws Exception {
        repository.save(new Manufacturer("Heineken", "Netherlands"));
        repository.save(new Manufacturer("Guinness", "Ireland"));
        repository.save(new Manufacturer("Asahi", "Japan"));

        mockMvc.perform(get("/api/manufacturers").param("country", "ire"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Guinness"));
    }

    @Test
    void search_combinedNameAndCountry_appliesAndSemantics() throws Exception {
        repository.save(new Manufacturer("Heineken", "Netherlands"));
        repository.save(new Manufacturer("Guinness", "Ireland"));
        repository.save(new Manufacturer("Asahi", "Japan"));

        mockMvc.perform(get("/api/manufacturers").param("name", "e").param("country", "ire"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Guinness"));
    }

    @Test
    void search_noFilters_returnsAllManufacturers() throws Exception {
        repository.save(new Manufacturer("Heineken", "Netherlands"));
        repository.save(new Manufacturer("Guinness", "Ireland"));
        repository.save(new Manufacturer("Asahi", "Japan"));

        mockMvc.perform(get("/api/manufacturers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void create_validRequest_returns201WithLocationAndBody() throws Exception {
        String location = mockMvc.perform(post("/api/manufacturers")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("Heineken", "Netherlands"))))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.name").value("Heineken"))
            .andExpect(jsonPath("$.country").value("Netherlands"))
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn().getResponse().getHeader("Location");

        assertThat(location).contains("/api/manufacturers/");
    }

    @Test
    void getById_existingManufacturer_returns200WithData() throws Exception {
        Manufacturer saved = repository.save(new Manufacturer("Guinness", "Ireland"));

        mockMvc.perform(get("/api/manufacturers/{id}", saved.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saved.getId()))
            .andExpect(jsonPath("$.name").value("Guinness"))
            .andExpect(jsonPath("$.country").value("Ireland"));
    }

    @Test
    void getById_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/manufacturers/{id}", 999L))
            .andExpect(status().isNotFound());
    }

    @Test
    void update_existingManufacturer_returns200WithUpdatedData() throws Exception {
        Manufacturer saved = repository.save(new Manufacturer("Old Name", "Old Country"));

        mockMvc.perform(put("/api/manufacturers/{id}", saved.getId())
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("New Name", "New Country"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.country").value("New Country"));
    }

    @Test
    void update_nonExistentId_returns404() throws Exception {
        mockMvc.perform(put("/api/manufacturers/{id}", 999L)
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("Name", "Country"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingManufacturer_returns204() throws Exception {
        Manufacturer saved = repository.save(new Manufacturer("Carlsberg", "Denmark"));

        mockMvc.perform(delete("/api/manufacturers/{id}", saved.getId())
                .with(httpBasic("admin", "pass")))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/manufacturers/{id}", 999L)
                .with(httpBasic("admin", "pass")))
            .andExpect(status().isNotFound());
    }

    @Test
    void create_blankName_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/manufacturers")
                .with(httpBasic("admin", "pass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("", "Netherlands"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
}
