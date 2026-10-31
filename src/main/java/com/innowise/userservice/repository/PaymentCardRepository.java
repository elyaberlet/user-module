package com.innowise.userservice.repository;

import com.innowise.userservice.entity.PaymentCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {

    Page<PaymentCard> findByUser_IdAndActiveTrue(Long userId, Pageable pageable);

    long countByUser_IdAndActiveTrue(Long userId);

    boolean existsByIdAndUser_Id(Long cardId, Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PaymentCard c set c.active = :active, c.updatedAt = CURRENT_TIMESTAMP where c.id = :id")
    void updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
}
