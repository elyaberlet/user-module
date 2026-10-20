package com.marketplace.userservice.specification;

import com.marketplace.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;

public class PaymentCardSpecification {

    public static Specification<PaymentCard> hasUserId(Long userId) {
        return (root, query, criteriaBuilder) ->
                userId == null ? null : criteriaBuilder.equal(root.get("user").get("id"), userId);
    }
}
