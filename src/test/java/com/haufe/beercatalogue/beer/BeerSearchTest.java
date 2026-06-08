package com.haufe.beercatalogue.beer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.haufe.beercatalogue.manufacturer.Manufacturer;
import com.haufe.beercatalogue.manufacturer.ManufacturerRepository;
import com.haufe.beercatalogue.security.AppUserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
class BeerSearchTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    BeerRepository beerRepository;

    @Autowired
    ManufacturerRepository manufacturerRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @BeforeEach
    void setUp() {
        beerRepository.deleteAll();
        appUserRepository.deleteAll();
        manufacturerRepository.deleteAll();

        Manufacturer m1 = manufacturerRepository.save(new Manufacturer("Heineken", "Netherlands"));
        Manufacturer m2 = manufacturerRepository.save(new Manufacturer("Guinness", "Ireland"));

        beerRepository.save(new Beer("Lager Classic", new BigDecimal("5.00"), BeerType.LAGER, null, m1));
        beerRepository.save(new Beer("IPA Hoppy", new BigDecimal("6.50"), BeerType.IPA, null, m1));
        beerRepository.save(new Beer("Stout Dark", new BigDecimal("4.20"), BeerType.STOUT, null, m2));
        beerRepository.save(new Beer("Stout Extra", new BigDecimal("7.50"), BeerType.STOUT, null, m2));
        beerRepository.save(new Beer("Wheat Ale", new BigDecimal("4.80"), BeerType.ALE, null, m1));
    }

    @Test
    void search_noFilters_returnsAllBeers() throws Exception {
        mockMvc.perform(get("/api/beers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void search_byName_caseInsensitive_returnsMatchingBeers() throws Exception {
        mockMvc.perform(get("/api/beers").param("name", "stout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].name").value("Stout Dark"))
            .andExpect(jsonPath("$.content[1].name").value("Stout Extra"));
    }

    @Test
    void search_byType_returnsOnlyMatchingType() throws Exception {
        mockMvc.perform(get("/api/beers").param("type", "IPA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("IPA Hoppy"));
    }

    @Test
    void search_byExactAbv_returnsExactMatch() throws Exception {
        mockMvc.perform(get("/api/beers").param("abv", "5.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Lager Classic"));
    }

    @Test
    void search_byAbvRange_returnsBeersInRange() throws Exception {
        mockMvc.perform(get("/api/beers").param("minAbv", "5.0").param("maxAbv", "7.5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void search_byManufacturerName_partialCaseInsensitive_returnsBeersOfMatchingManufacturer() throws Exception {
        mockMvc.perform(get("/api/beers").param("manufacturerName", "inn"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void search_combinedTypeAndMinAbv_appliesAndSemantics() throws Exception {
        mockMvc.perform(get("/api/beers").param("type", "STOUT").param("minAbv", "5.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Stout Extra"));
    }

    @Test
    void search_pagination_firstPage() throws Exception {
        mockMvc.perform(get("/api/beers").param("size", "2").param("page", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false))
            .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void search_pagination_secondPage() throws Exception {
        mockMvc.perform(get("/api/beers").param("size", "2").param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(false))
            .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void search_sortByAbvDesc_returnsBeersInDescendingAbvOrder() throws Exception {
        mockMvc.perform(get("/api/beers").param("sort", "abv,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Stout Extra"))
            .andExpect(jsonPath("$.content[4].name").value("Stout Dark"));
    }
}
