package com.haufe.beercatalogue.manufacturer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class ManufacturerControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ManufacturerRepository repository;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void list_emptyDatabase_returns200AndEmptyArray() throws Exception {
        mockMvc.perform(get("/api/manufacturers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void create_validRequest_returns201WithLocationAndBody() throws Exception {
        String location = mockMvc.perform(post("/api/manufacturers")
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("New Name", "New Country"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.country").value("New Country"));
    }

    @Test
    void update_nonExistentId_returns404() throws Exception {
        mockMvc.perform(put("/api/manufacturers/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("Name", "Country"))))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingManufacturer_returns204() throws Exception {
        Manufacturer saved = repository.save(new Manufacturer("Carlsberg", "Denmark"));

        mockMvc.perform(delete("/api/manufacturers/{id}", saved.getId()))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/manufacturers/{id}", 999L))
            .andExpect(status().isNotFound());
    }

    @Test
    void create_blankName_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/manufacturers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ManufacturerRequest("", "Netherlands"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
}
