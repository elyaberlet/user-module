package com.marketplace.userservice.specification;

import com.marketplace.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> hasName(String name) {
        return (root, query, criteriaBuilder) ->
                name == null ? null : criteriaBuilder.equal(root.get("name"), name);
    }

    public static Specification<User> hasSurname(String surname) {
        return (root, query, criteriaBuilder) ->
                surname == null ? null : criteriaBuilder.equal(root.get("surname"), surname);
    }

    public static Specification<User> filterByNameAndSurname(String name, String surname) {
        return Specification.where(hasName(name))
                .and(hasSurname(surname));
    }
}