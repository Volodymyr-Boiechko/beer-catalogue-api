package com.haufe.beercatalogue.beer;

import java.math.BigDecimal;

public record BeerSearchCriteria(
    String name,
    BeerType type,
    BigDecimal abv,
    BigDecimal minAbv,
    BigDecimal maxAbv,
    String manufacturerName
) {}
