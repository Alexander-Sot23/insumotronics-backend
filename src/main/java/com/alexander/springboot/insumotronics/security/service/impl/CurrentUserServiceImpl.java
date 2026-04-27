package com.alexander.springboot.insumotronics.security.service.impl;

import com.alexander.springboot.insumotronics.entity.MyUser;
import com.alexander.springboot.insumotronics.repository.MyUserRepository;
import com.alexander.springboot.insumotronics.security.service.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    @Autowired
    private MyUserRepository myUserRepository;

    @Override
    public MyUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        } else {
            throw new RuntimeException("Unable to get user details from authentication");
        }

        return myUserRepository.findByCode(username)
                .orElseThrow(() -> new RuntimeException("Admin user not found: " + username));

    }

    @Override
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    @Override
    public boolean hasRole(String role) {
        MyUser user = getCurrentUser();
        return user.getRole() != null && user.getRole().equals(role);
    }
}

