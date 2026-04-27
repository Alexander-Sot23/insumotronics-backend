package com.alexander.springboot.insumotronics.service;

import com.alexander.springboot.insumotronics.model.CartM;
import com.alexander.springboot.insumotronics.model.CreateCartM;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CartService {
    Page<CartM> findAll(Pageable pageable);
    Optional<CartM> findById(UUID id);
    Page<CartM> findByUserId(UUID userId, Pageable pageable);
    CartM getOrCreateByUserId(UUID userId);
    CartM create(CreateCartM cartM);
    void delete(UUID id);
}

