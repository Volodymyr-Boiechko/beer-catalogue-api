package com.haufe.beercatalogue.beer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Style/category of the beer")
public enum BeerType {
    IPA, LAGER, STOUT, PILSNER, ALE, OTHER
}
