package com.haufe.beercatalogue.manufacturer;

import org.springframework.test.util.ReflectionTestUtils;

public final class ManufacturerTestFactory {

    private ManufacturerTestFactory() {
    }

    public static Manufacturer manufacturer(Long id, String name, String country) {
        var m = new Manufacturer(name, country);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }
}
