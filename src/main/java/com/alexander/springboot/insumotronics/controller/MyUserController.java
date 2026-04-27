package com.alexander.springboot.insumotronics.controller;

/*
 * Este controlador funciona tanto para los usuarios con rol STUDENT, TEACHER y ADMIN
 */

import com.alexander.springboot.insumotronics.dto.MyUserDTO;
import com.alexander.springboot.insumotronics.exception.MyUserNotFoundException;
import com.alexander.springboot.insumotronics.service.MyUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/user")
public class MyUserController {

    @Autowired
    private MyUserService service;

    @GetMapping("/code")
    public ResponseEntity<MyUserDTO> getByCode(@RequestParam String code){
        return service.findByCode(code)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new MyUserNotFoundException("User with code:" + code + ", not found."));
    }
}
