package com.alexander.springboot.insumotronics.controller.admin;

/*
 * Este controlador solo funciona para los usuarios con el rol ADMIN
 */

import com.alexander.springboot.insumotronics.dto.MyUserDTO;
import com.alexander.springboot.insumotronics.exception.MyUserNotFoundException;
import com.alexander.springboot.insumotronics.service.MyUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/user")
public class MyUserControllerADMIN {

    @Autowired
    private MyUserService service;

    @GetMapping
    public ResponseEntity<Page<MyUserDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdDate") String sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/id")
    public ResponseEntity<MyUserDTO> getById(@RequestParam UUID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new MyUserNotFoundException("User with id:" + id + ", not found."));
    }

    @GetMapping("/code")
    public ResponseEntity<MyUserDTO> getByCode(@RequestParam String code){
        return service.findByCode(code)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new MyUserNotFoundException("User with code:" + code + ", not found."));
    }

    @GetMapping("/email")
    public ResponseEntity<MyUserDTO> getByEmail(@RequestParam String email){
        return service.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new MyUserNotFoundException("User with email:" + email + ", not found."));
    }
}
