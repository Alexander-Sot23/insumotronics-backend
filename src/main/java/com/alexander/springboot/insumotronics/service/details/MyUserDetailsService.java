package com.alexander.springboot.insumotronics.service.details;

import com.alexander.springboot.insumotronics.entity.MyUser;
import com.alexander.springboot.insumotronics.repository.MyUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private MyUserRepository myUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<MyUser> userOptional = myUserRepository.findByCode(username);

        if (userOptional.isPresent()) {
            MyUser myUser = userOptional.get();

            List<GrantedAuthority> authorities = getAuthorities(myUser);

            return User.builder()
                    .username(myUser.getCode())
                    .password(myUser.getPasswordHash())
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .build();
        } else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }

    private List<GrantedAuthority> getAuthorities(MyUser myUser) {
        // Convertir el enum role a SimpleGrantedAuthority
        String roleWithPrefix = "ROLE_" + myUser.getRole().name();

        return List.of(new SimpleGrantedAuthority(roleWithPrefix));
    }
}

