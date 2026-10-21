package com.marketplace.userservice.repository;

import com.marketplace.userservice.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByIdAndActiveTrue(Long id);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "paymentCards")
    @Query("select u from User u where u.id = :id and u.active = true")
    Optional<User> findActiveWithCardsById(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update User u set u.active = :active, u.updatedAt = CURRENT_TIMESTAMP where u.id = :id")
    int updateActiveStatus(@Param("id") Long id, @Param("active") boolean active);
}
