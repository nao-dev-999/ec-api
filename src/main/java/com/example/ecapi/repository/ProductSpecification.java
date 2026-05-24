package com.example.ecapi.repository;

import com.example.ecapi.entity.Product;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {
    public static Specification<Product> byCriteria(
            String name, String description, BigDecimal price) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (description != null) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("description")),
                                "%" + description.toLowerCase() + "%"));
            }
            if (price != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), price));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
