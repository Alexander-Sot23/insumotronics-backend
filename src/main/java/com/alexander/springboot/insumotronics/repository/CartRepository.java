package com.alexander.springboot.insumotronics.repository;

import com.alexander.springboot.insumotronics.entity.Cart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Page<Cart> findByMyUserId(UUID myUserId, Pageable pageable);
    Optional<Cart> findTopByMyUserIdOrderByCreationDateDesc(UUID myUserId);
}

