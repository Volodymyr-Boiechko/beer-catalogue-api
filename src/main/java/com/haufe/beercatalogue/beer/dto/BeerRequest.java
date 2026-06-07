package com.haufe.beercatalogue.beer.dto;

import com.haufe.beercatalogue.beer.Beer;
import com.haufe.beercatalogue.beer.BeerType;
import com.haufe.beercatalogue.manufacturer.Manufacturer;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BeerRequest(
    @NotBlank String name,
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal abv,
    @NotNull BeerType type,
    String description,
    @NotNull Long manufacturerId
) {

    public Beer toEntity(Manufacturer manufacturer) {
        return new Beer(name, abv, type, description, manufacturer);
    }

    public void applyTo(Beer entity, Manufacturer manufacturer) {
        entity.setName(name);
        entity.setAbv(abv);
        entity.setType(type);
        entity.setDescription(description);
        entity.setManufacturer(manufacturer);
    }
}
