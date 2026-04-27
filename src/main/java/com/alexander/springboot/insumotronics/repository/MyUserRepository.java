package com.alexander.springboot.insumotronics.repository;

import com.alexander.springboot.insumotronics.entity.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MyUserRepository extends JpaRepository<MyUser, UUID> {
    Optional<MyUser> findByCode(String code);
    Optional<MyUser> findByEmail(String email);
}

