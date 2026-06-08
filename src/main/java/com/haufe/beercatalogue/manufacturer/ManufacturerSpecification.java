package com.haufe.beercatalogue.manufacturer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;

public class ManufacturerSpecification {

    private ManufacturerSpecification() {
    }

    public static Specification<Manufacturer> from(ManufacturerSearchCriteria criteria) {
        List<Specification<Manufacturer>> specs = new ArrayList<>();
        if (Objects.nonNull(criteria.name())) {
            specs.add(nameLike(criteria.name()));
        }
        if (Objects.nonNull(criteria.country())) {
            specs.add(countryLike(criteria.country()));
        }
        return Specification.allOf(specs);
    }

    private static Specification<Manufacturer> nameLike(String name) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<Manufacturer> countryLike(String country) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("country")), "%" + country.toLowerCase() + "%");
    }
}
