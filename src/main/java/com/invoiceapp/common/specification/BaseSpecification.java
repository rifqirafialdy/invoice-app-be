package com.invoiceapp.common.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BaseSpecification {

    public static <T> Specification<T> withUserId(UUID userId, String userFieldName) {
        return (root, query, cb) -> {
            if (userId == null) return cb.conjunction();
            return cb.equal(root.get(userFieldName).get("id"), userId);
        };
    }

    public static <T> Specification<T> withSearch(String search, String... fields) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            String searchPattern = "%" + search.toLowerCase() + "%";

            for (String field : fields) {
                predicates.add(cb.like(cb.lower(root.get(field)), searchPattern));
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static <T, V> Specification<T> withEquals(String field, V value) {
        return (root, query, cb) -> {
            if (value == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get(field), value);
        };
    }

    public static <T, V extends Comparable<? super V>> Specification<T> withDateRange(String field, V startDate, V endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(field), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(field), endDate));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}