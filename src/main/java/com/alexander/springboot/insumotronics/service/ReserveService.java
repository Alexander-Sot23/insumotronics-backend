package com.alexander.springboot.insumotronics.service;

import com.alexander.springboot.insumotronics.enums.ReserveStatus;
import com.alexander.springboot.insumotronics.model.ReserveM;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReserveService {
    long countTotalReservations();
    long countByUserId(UUID userId);
    long countByUserIdAndStatus(UUID userId, ReserveStatus status);
    List<ReserveM> findActiveReserves(UUID userId);
    Page<ReserveM> findAll(Pageable pageable);
    Page<ReserveM> findByStatus(String status, Pageable pageable);
    Page<ReserveM> findByUserId(UUID userId, Pageable pageable);
    Page<ReserveM> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);
    ReserveM findById(UUID id);
    ReserveM checkoutCart(UUID cartId);
    ReserveM confirmReserve(UUID id);
    ReserveM cancelReserve(UUID id, String message);
}

