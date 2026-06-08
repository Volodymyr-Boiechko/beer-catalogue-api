package com.haufe.beercatalogue.beer;

import com.haufe.beercatalogue.beer.dto.BeerRequest;
import com.haufe.beercatalogue.beer.dto.BeerResponse;
import com.haufe.beercatalogue.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/beers")
public class BeerController {

    private final BeerService service;

    public BeerController(BeerService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<BeerResponse> list(
        @ParameterObject BeerSearchCriteria criteria,
        @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return service.search(criteria, pageable);
    }

    @GetMapping("/{id}")
    public BeerResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<BeerResponse> create(@Valid @RequestBody BeerRequest request) {
        BeerResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public BeerResponse update(@PathVariable Long id, @Valid @RequestBody BeerRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
