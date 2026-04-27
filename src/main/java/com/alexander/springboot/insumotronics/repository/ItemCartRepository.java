package com.alexander.springboot.insumotronics.repository;

import com.alexander.springboot.insumotronics.entity.ItemCart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItemCartRepository extends JpaRepository<ItemCart, UUID> {
    Page<ItemCart> findByCartId(UUID cartId, Pageable pageable);
    Optional<ItemCart> findByCartIdAndProductId(UUID cartId, UUID productId);
}

