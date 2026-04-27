package com.alexander.springboot.insumotronics.security.service;

import com.alexander.springboot.insumotronics.entity.MyUser;

import java.util.UUID;

public interface CurrentUserService {
    MyUser getCurrentUser();
    UUID getCurrentUserId();
    boolean hasRole(String role);
}

