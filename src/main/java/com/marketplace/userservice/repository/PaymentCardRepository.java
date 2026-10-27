package com.marketplace.userservice.repository;

import com.marketplace.userservice.entity.PaymentCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {
    List<PaymentCard> findByUser_IdAndActiveTrue(Long userId);

    Page<PaymentCard> findByUser_IdAndActiveTrue(Long userId, Pageable pageable);

    long countByUser_IdAndActiveTrue(Long userId);

    boolean existsByIdAndUser_Id(Long cardId, Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PaymentCard c set c.active = :active, c.updatedAt = CURRENT_TIMESTAMP where c.id = :id")
    int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
}
