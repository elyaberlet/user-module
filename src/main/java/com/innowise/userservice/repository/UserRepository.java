package com.innowise.userservice.repository;

import com.innowise.userservice.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
