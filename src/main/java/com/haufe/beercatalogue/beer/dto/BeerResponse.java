package com.haufe.beercatalogue.beer.dto;

import com.haufe.beercatalogue.beer.Beer;
import com.haufe.beercatalogue.beer.BeerType;

import java.math.BigDecimal;
import java.util.List;

public record BeerResponse(
    Long id,
    String name,
    BigDecimal abv,
    BeerType type,
    String description,
    Long manufacturerId,
    String manufacturerName
) {

    public static BeerResponse from(Beer entity) {
        return new BeerResponse(
            entity.getId(),
            entity.getName(),
            entity.getAbv(),
            entity.getType(),
            entity.getDescription(),
            entity.getManufacturer().getId(),
            entity.getManufacturer().getName()
        );
    }

    public static List<BeerResponse> from(List<Beer> entities) {
        return entities.stream().map(BeerResponse::from).toList();
    }
}
