package com.alexander.springboot.insumotronics.repository;

import com.alexander.springboot.insumotronics.entity.Reserve;
import com.alexander.springboot.insumotronics.enums.ReserveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReserveRepository extends JpaRepository<Reserve, UUID> {
    Page<Reserve> findByMyUserId(UUID myUserId, Pageable pageable);
    Page<Reserve> findByStatus(ReserveStatus status, Pageable pageable);
    Page<Reserve> findByMyUserIdAndStatus(UUID myUserId, ReserveStatus status, Pageable pageable);
    long countByMyUserId(UUID myUserId);
    long countByMyUserIdAndStatus(UUID myUserId, ReserveStatus status);
    List<Reserve> findByMyUserIdAndStatusIn(UUID myUserId, List<ReserveStatus> statuses);
}
