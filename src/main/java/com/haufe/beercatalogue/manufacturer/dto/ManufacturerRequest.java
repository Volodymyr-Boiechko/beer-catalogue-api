package com.haufe.beercatalogue.manufacturer.dto;

import com.haufe.beercatalogue.manufacturer.Manufacturer;
import jakarta.validation.constraints.NotBlank;

public record ManufacturerRequest(
    @NotBlank String name,
    @NotBlank String country
) {

    public Manufacturer toEntity() {
        return new Manufacturer(name, country);
    }

    public void applyTo(Manufacturer entity) {
        entity.setName(name);
        entity.setCountry(country);
    }
}
