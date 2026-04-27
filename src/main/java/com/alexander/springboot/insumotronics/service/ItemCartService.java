package com.alexander.springboot.insumotronics.service;

import com.alexander.springboot.insumotronics.model.ItemCartM;
import com.alexander.springboot.insumotronics.model.CreateItemCartM;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ItemCartService {
    Page<ItemCartM> findAll(Pageable pageable);
    Optional<ItemCartM> findById(UUID id);
    Page<ItemCartM> findByCartId(UUID cartId, Pageable pageable);
    ItemCartM create(CreateItemCartM itemCartM);
    ItemCartM addToCart(UUID userId, UUID productId, int quantity);
    ItemCartM updateQuantity(UUID id, int quantity);
    void delete(UUID id);
}

