package com.haufe.beercatalogue.manufacturer;

import com.haufe.beercatalogue.common.dto.PageResponse;
import com.haufe.beercatalogue.common.exception.ConflictException;
import com.haufe.beercatalogue.common.exception.NotFoundException;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerRequest;
import com.haufe.beercatalogue.manufacturer.dto.ManufacturerResponse;
import com.haufe.beercatalogue.security.OwnershipChecker;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ManufacturerService {

    private final ManufacturerRepository repository;
    private final OwnershipChecker ownershipChecker;

    public ManufacturerService(ManufacturerRepository repository, OwnershipChecker ownershipChecker) {
        this.repository = repository;
        this.ownershipChecker = ownershipChecker;
    }

    public ManufacturerResponse create(ManufacturerRequest request) {
        if (repository.existsByNameAndCountry(request.name(), request.country())) {
            throw new ConflictException("A manufacturer with this name and country already exists.");
        }
        var saved = repository.save(request.toEntity());
        return ManufacturerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ManufacturerResponse getById(Long id) {
        var entity = findById(id);
        return ManufacturerResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<ManufacturerResponse> search(ManufacturerSearchCriteria criteria, Pageable pageable) {
        var all = repository.findAll(ManufacturerSpecification.from(criteria), pageable)
                .map(ManufacturerResponse::from);
        return PageResponse.from(all);
    }

    public ManufacturerResponse update(Long id, ManufacturerRequest request) {
        var found = findById(id);
        if (!ownershipChecker.canEditManufacturer(id)) {
            throw new AccessDeniedException("Not authorized to update this manufacturer");
        }
        assertNoConflict(found, request);
        request.applyTo(found);
        var saved = repository.save(found);
        return ManufacturerResponse.from(saved);
    }

    private void assertNoConflict(Manufacturer found, ManufacturerRequest request) {
        boolean keyChanged = !found.getName().equals(request.name())
                || !found.getCountry().equals(request.country());
        if (keyChanged && repository.existsByNameAndCountry(request.name(), request.country())) {
            throw new ConflictException("A manufacturer with this name and country already exists.");
        }
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Manufacturer not found: " + id);
        }
        repository.deleteById(id);
    }

    private Manufacturer findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Manufacturer not found: " + id));
    }
}
