package com.haufe.beercatalogue.manufacturer.dto;

import com.haufe.beercatalogue.manufacturer.Manufacturer;
import java.util.List;

public record ManufacturerResponse(
    Long id,
    String name,
    String country
) {

    public static ManufacturerResponse from(Manufacturer entity) {
        return new ManufacturerResponse(entity.getId(), entity.getName(), entity.getCountry());
    }

    public static List<ManufacturerResponse> from(List<Manufacturer> entities) {
        return entities.stream().map(ManufacturerResponse::from).toList();
    }
}
