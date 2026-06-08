package com.haufe.beercatalogue.beer;

import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class BeerSpecification {

    private BeerSpecification() {
    }

    public static Specification<Beer> from(BeerSearchCriteria criteria) {
        List<Specification<Beer>> specs = new ArrayList<>();
        if (criteria.name() != null) {
            specs.add(nameLike(criteria.name()));
        }
        if (criteria.type() != null) {
            specs.add(typeEquals(criteria.type()));
        }
        if (criteria.abv() != null) {
            specs.add(abvEquals(criteria.abv()));
        }
        if (criteria.minAbv() != null) {
            specs.add(abvGte(criteria.minAbv()));
        }
        if (criteria.maxAbv() != null) {
            specs.add(abvLte(criteria.maxAbv()));
        }
        if (criteria.manufacturerName() != null) {
            specs.add(manufacturerNameLike(criteria.manufacturerName()));
        }
        return Specification.allOf(specs);
    }

    private static Specification<Beer> nameLike(String name) {
        return (root, query, cb) ->
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<Beer> typeEquals(BeerType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    private static Specification<Beer> abvEquals(BigDecimal abv) {
        return (root, query, cb) -> cb.equal(root.get("abv"), abv);
    }

    private static Specification<Beer> abvGte(BigDecimal minAbv) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("abv"), minAbv);
    }

    private static Specification<Beer> abvLte(BigDecimal maxAbv) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("abv"), maxAbv);
    }

    private static Specification<Beer> manufacturerNameLike(String name) {
        return (root, query, cb) -> {
            var join = root.join("manufacturer", JoinType.INNER);
            return cb.like(cb.lower(join.get("name")), "%" + name.toLowerCase() + "%");
        };
    }
}
