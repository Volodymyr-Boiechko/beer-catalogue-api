package com.haufe.beercatalogue.manufacturer;

import com.haufe.beercatalogue.exception.NotFoundException;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerRequest;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ManufacturerService {

    private final ManufacturerRepository repository;

    public ManufacturerService(ManufacturerRepository repository) {
        this.repository = repository;
    }

    public ManufacturerResponse create(ManufacturerRequest request) {
        var saved = repository.save(request.toEntity());
        return ManufacturerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ManufacturerResponse getById(Long id) {
        var entity = findById(id);
        return ManufacturerResponse.from(entity);
    }

    private Manufacturer findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Manufacturer not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ManufacturerResponse> findAll() {
        var all = repository.findAll();
        return ManufacturerResponse.from(all);
    }

    public ManufacturerResponse update(Long id, ManufacturerRequest request) {
        var found = findById(id);
        request.applyTo(found);
        var saved = repository.save(found);
        return ManufacturerResponse.from(saved);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Manufacturer not found: " + id);
        }
        repository.deleteById(id);
    }
}
