package com.alexander.springboot.insumotronics.service;

import com.alexander.springboot.insumotronics.dto.MyUserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface MyUserService {
    Page<MyUserDTO> findAll(Pageable pageable);
    Optional<MyUserDTO> findById(UUID id);
    Optional<MyUserDTO> findByCode(String code);
    Optional<MyUserDTO> findByEmail(String email);
}

