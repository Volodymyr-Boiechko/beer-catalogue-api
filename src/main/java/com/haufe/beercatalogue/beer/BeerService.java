package com.haufe.beercatalogue.beer;

import com.haufe.beercatalogue.beer.dto.BeerRequest;
import com.haufe.beercatalogue.beer.dto.BeerResponse;
import com.haufe.beercatalogue.common.exception.NotFoundException;
import com.haufe.beercatalogue.manufacturer.Manufacturer;
import com.haufe.beercatalogue.manufacturer.ManufacturerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BeerService {

    private final BeerRepository beerRepository;
    private final ManufacturerRepository manufacturerRepository;

    public BeerService(BeerRepository beerRepository, ManufacturerRepository manufacturerRepository) {
        this.beerRepository = beerRepository;
        this.manufacturerRepository = manufacturerRepository;
    }

    public BeerResponse create(BeerRequest request) {
        var manufacturer = resolveManufacturer(request.manufacturerId());
        var saved = beerRepository.save(request.toEntity(manufacturer));
        return BeerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public BeerResponse getById(Long id) {
        return BeerResponse.from(findBeerById(id));
    }

    @Transactional(readOnly = true)
    public List<BeerResponse> findAll() {
        return BeerResponse.from(beerRepository.findAll());
    }

    public BeerResponse update(Long id, BeerRequest request) {
        var beer = findBeerById(id);
        var manufacturer = resolveManufacturer(request.manufacturerId());
        request.applyTo(beer, manufacturer);
        return BeerResponse.from(beerRepository.save(beer));
    }

    public void delete(Long id) {
        if (!beerRepository.existsById(id)) {
            throw new NotFoundException("Beer not found: " + id);
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
