package com.alexander.springboot.insumotronics.service.impl;

import com.alexander.springboot.insumotronics.dto.MyUserDTO;
import com.alexander.springboot.insumotronics.entity.MyUser;
import com.alexander.springboot.insumotronics.repository.MyUserRepository;
import com.alexander.springboot.insumotronics.service.MyUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class MyUserServiceImpl implements MyUserService {

    private final MyUserRepository repository;

    public MyUserServiceImpl(MyUserRepository repository){
        this.repository = repository;
    }

    @Override
    public Page<MyUserDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::convertToDTO);
    }

    @Override
    public Optional<MyUserDTO> findById(UUID id) {
        return repository.findById(id).map(this::convertToDTO);
    }

    @Override
    public Optional<MyUserDTO> findByCode(String code) {
        return repository.findByCode(code).map(this::convertToDTO);
    }

    @Override
    public Optional<MyUserDTO> findByEmail(String email) {
        return repository.findByEmail(email).map(this::convertToDTO);
    }

    private MyUserDTO convertToDTO(MyUser user) {
        return new MyUserDTO(
                user.getId(),
                user.getName(),
                user.getLastname(),
                user.getCode(),
                user.getAcademicLevel(),
                user.getCareer(),
                user.getDegree(),
                user.getRole(),
                user.getStatus(),
                user.getEmail(),
                user.getFirstLogin(),
                user.getLastLogin(),
                user.getCreatedDate(),
                user.getUpdateDate()
        );
    }
}

