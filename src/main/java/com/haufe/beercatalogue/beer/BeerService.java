package com.haufe.beercatalogue.beer;

import com.haufe.beercatalogue.beer.dto.BeerRequest;
import com.haufe.beercatalogue.beer.dto.BeerResponse;
import com.haufe.beercatalogue.common.dto.PageResponse;
import com.haufe.beercatalogue.common.exception.NotFoundException;
import com.haufe.beercatalogue.manufacturer.Manufacturer;
import com.haufe.beercatalogue.manufacturer.ManufacturerRepository;
import com.haufe.beercatalogue.security.OwnershipChecker;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BeerService {

    private final BeerRepository beerRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final OwnershipChecker ownershipChecker;

    public BeerService(BeerRepository beerRepository,
                       ManufacturerRepository manufacturerRepository,
                       OwnershipChecker ownershipChecker) {
        this.beerRepository = beerRepository;
        this.manufacturerRepository = manufacturerRepository;
        this.ownershipChecker = ownershipChecker;
    }

    public BeerResponse create(BeerRequest request) {
        if (!ownershipChecker.canEditManufacturer(request.manufacturerId())) {
            throw new AccessDeniedException("Not authorized to create beer for this manufacturer");
        }
        var manufacturer = resolveManufacturer(request.manufacturerId());
        var saved = beerRepository.save(request.toEntity(manufacturer));
        return BeerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public BeerResponse getById(Long id) {
        return BeerResponse.from(findBeerById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<BeerResponse> search(BeerSearchCriteria criteria, Pageable pageable) {
        var all = beerRepository.findAll(BeerSpecification.from(criteria), pageable)
            .map(BeerResponse::from);
        return PageResponse.from(all);
    }

    public BeerResponse update(Long id, BeerRequest request) {
        var beer = findBeerById(id);
        if (!ownershipChecker.canEditManufacturer(beer.getManufacturer().getId())
                || !ownershipChecker.canEditManufacturer(request.manufacturerId())) {
            throw new AccessDeniedException("Not authorized to update this beer");
        }
        var manufacturer = resolveManufacturer(request.manufacturerId());
        request.applyTo(beer, manufacturer);
        return BeerResponse.from(beerRepository.save(beer));
    }

    public void delete(Long id) {
        var beer = findBeerById(id);
        if (!ownershipChecker.canEditManufacturer(beer.getManufacturer().getId())) {
            throw new AccessDeniedException("Not authorized to delete this beer");
        }
        beerRepository.deleteById(id);
    }

    private Beer findBeerById(Long id) {
        return beerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Beer not found: " + id));
    }

    private Manufacturer resolveManufacturer(Long id) {
        return manufacturerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Manufacturer not found: " + id));
    }
}
