package com.alexander.springboot.insumotronics.repository;

import com.alexander.springboot.insumotronics.entity.ItemReserve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemReserveRepository extends JpaRepository<ItemReserve, UUID> {
}

