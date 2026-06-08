package com.haufe.beercatalogue.beer;

import com.haufe.beercatalogue.manufacturer.Manufacturer;
import java.math.BigDecimal;
import org.springframework.test.util.ReflectionTestUtils;

public final class BeerTestFactory {

    private BeerTestFactory() {
    }

    public static Beer beer(
        Long id,
        String name,
        BigDecimal abv,
        BeerType type,
        String description,
        Manufacturer manufacturer
    ) {
        var b = new Beer(name, abv, type, description, manufacturer);
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }
}
