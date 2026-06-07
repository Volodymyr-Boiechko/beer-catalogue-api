package com.haufe.beercatalogue.manufacturer;

import com.haufe.beercatalogue.manufacturer.dto.ManufacturerRequest;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manufacturers")
public class ManufacturerController {

    private final ManufacturerService service;

    public ManufacturerController(ManufacturerService service) {
        this.service = service;
    }

    @GetMapping
    public List<ManufacturerResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ManufacturerResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManufacturerResponse create(@Valid @RequestBody ManufacturerRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ManufacturerResponse update(
        @PathVariable Long id,
        @Valid @RequestBody ManufacturerRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
