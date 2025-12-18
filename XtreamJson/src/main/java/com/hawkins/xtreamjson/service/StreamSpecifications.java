package com.hawkins.xtreamjson.service;

import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public class StreamSpecifications {

    public static <T> Specification<T> hasCategoryId(String categoryId) {
        return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
    }

    public static <T> Specification<T> nameStartsWith(String letter) {
        return (root, query, cb) -> {
            if (letter == null || letter.isEmpty()) {
                return cb.conjunction();
            }
            // Case-insensitive check for name starting with letter
            // We use cleanTitle logic approximation or just simple LIKE if cleanTitle is
            // too complex for DB
            // For simplicity and performance, we'll assume the name in DB starts with the
            // letter or we just check standard start
            // If the DB names have prefixes like "EN | ", this simple LIKE might fail.
            // However, doing complex regex in DB is slow/non-portable.
            // Let's try a simple LIKE first as per the plan.
            // If strict cleanTitle matching is needed, it might be hard in pure SQL without
            // stored procs.
            // But looking at the existing code:
            // "SELECT m FROM MovieStream m WHERE m.categoryId = :categoryId AND
            // LOWER(m.name) LIKE LOWER(CONCAT(:letter, '%'))"
            // The existing repository method used simple LIKE. So we will stick to that.
            return cb.like(cb.lower(root.get("name")), letter.toLowerCase() + "%");
        };
    }

    public static <T> Specification<T> isIncluded(Set<String> includedSet) {
        return (root, query, cb) -> {
            if (includedSet == null || includedSet.isEmpty()) {
                return cb.conjunction();
            }

            java.util.List<Predicate> allPredicates = new java.util.ArrayList<>();

            for (String prefix : includedSet) {
                String upperPrefix = prefix.toUpperCase();
                for (String sep : com.hawkins.xtreamjson.util.XtreamCodesUtils.SEPARATORS) {
                    allPredicates.add(cb.like(cb.upper(root.get("name")), upperPrefix + sep + "%"));
                }
                // Also match exact name if it's just the prefix
                allPredicates.add(cb.equal(cb.upper(root.get("name")), upperPrefix));
            }

            return cb.or(allPredicates.toArray(new Predicate[0]));
        };
    }

    public static <T> Specification<T> nameContains(String queryStr) {
        return (root, query, cb) -> {
            if (queryStr == null || queryStr.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + queryStr.toLowerCase() + "%");
        };
    }
}
