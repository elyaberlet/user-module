
package com.innowise.userservice.security;

import com.innowise.userservice.repository.PaymentCardRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("resourceAccessService")
public class ResourceAccessService {

    private final PaymentCardRepository cardRepository;

    public ResourceAccessService(PaymentCardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public boolean ownsCard(Long cardId) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return false;
        }

        Object claim = jwtAuth.getToken().getClaim("user_id");

        if (!(claim instanceof Number number)) {
            return false;
        }

        Long currentUserId = number.longValue();

        return cardRepository.existsByIdAndUser_Id(cardId, currentUserId);
    }
}
