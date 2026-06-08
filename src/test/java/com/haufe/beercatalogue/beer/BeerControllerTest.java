package com.haufe.beercatalogue.beer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haufe.beercatalogue.beer.dto.BeerRequest;
import com.haufe.beercatalogue.manufacturer.Manufacturer;
import com.haufe.beercatalogue.manufacturer.ManufacturerRepository;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.NONE)
class BeerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    ManufacturerRepository manufacturerRepository;

    @Autowired
    ObjectMapper objectMapper;

    Manufacturer manufacturer;

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
        manufacturerRepository.deleteAll();
        manufacturer = manufacturerRepository.save(new Manufacturer("Heineken", "Netherlands"));
    }

    @Test
    void list_emptyDatabase_returns200AndEmptyPage() throws Exception {
        mockMvc.perform(get("/api/beers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void create_validRequest_returns201WithLocationAndBody() throws Exception {
        BeerRequest request = new BeerRequest("Corona", new BigDecimal("4.50"),
            BeerType.LAGER, "Refreshing", manufacturer.getId());

        String location = mockMvc.perform(post("/api/beers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.name").value("Corona"))
            .andExpect(jsonPath("$.abv").value(4.50))
            .andExpect(jsonPath("$.type").value("LAGER"))
            .andExpect(jsonPath("$.manufacturerId").value(manufacturer.getId()))
            .andExpect(jsonPath("$.manufacturerName").value("Heineken"))
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn().getResponse().getHeader("Location");

        assertThat(location).contains("/api/beers/");
    }

    @ParameterizedTest
    @MethodSource("invalidBeerRequests")
    void create_invalidRequest_returns400(BeerRequest request, String expectedField) throws Exception {
        mockMvc.perform(post("/api/beers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors." + expectedField).exists());
    }

    static Stream<Arguments> invalidBeerRequests() {
        return Stream.of(
            Arguments.of(new BeerRequest("", new BigDecimal("5.0"), BeerType.IPA, "d", 1L), "name"),
            Arguments.of(new BeerRequest("X", null, BeerType.IPA, "d", 1L), "abv"),
            Arguments.of(new BeerRequest("X", new BigDecimal("150.0"), BeerType.IPA, "d", 1L), "abv"),
            Arguments.of(new BeerRequest("X", new BigDecimal("5.0"), null, "d", 1L), "type"),
            Arguments.of(new BeerRequest("X", new BigDecimal("5.0"), BeerType.IPA, "d", null), "manufacturerId")
        );
    }

    @Test
    void getById_existingBeer_returns200WithData() throws Exception {
        Beer saved = beerRepository.save(
            new Beer("Stout", new BigDecimal("4.20"), BeerType.STOUT, "Dark", manufacturer));

        mockMvc.perform(get("/api/beers/{id}", saved.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saved.getId()))
            .andExpect(jsonPath("$.name").value("Stout"))
            .andExpect(jsonPath("$.type").value("STOUT"))
            .andExpect(jsonPath("$.manufacturerName").value("Heineken"));
    }

    @Test
    void getById_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/beers/{id}", 999L))
            .andExpect(status().isNotFound());
    }

    @Test
    void update_existingBeer_returns200WithUpdatedData() throws Exception {
        Beer saved = beerRepository.save(
            new Beer("Old Name", new BigDecimal("4.0"), BeerType.ALE, null, manufacturer));
        BeerRequest request = new BeerRequest("New Name", new BigDecimal("5.5"),
            BeerType.IPA, "Updated", manufacturer.getId());

        mockMvc.perform(put("/api/beers/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.type").value("IPA"))
            .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    void update_nonExistentId_returns404() throws Exception {
        BeerRequest request = new BeerRequest("Beer", new BigDecimal("4.5"),
            BeerType.LAGER, null, manufacturer.getId());

        mockMvc.perform(put("/api/beers/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingBeer_returns204() throws Exception {
        Beer saved = beerRepository.save(
            new Beer("Pilsner", new BigDecimal("5.0"), BeerType.PILSNER, null, manufacturer));

        mockMvc.perform(delete("/api/beers/{id}", saved.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/beers/{id}", 999L))
            .andExpect(status().isNotFound());
    }
}
