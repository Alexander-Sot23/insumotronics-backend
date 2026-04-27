package com.alexander.springboot.insumotronics.controller;

/*
 * Este controlador funciona tanto para los usuarios con rol STUDENT, TEACHER y ADMIN
 */

import com.alexander.springboot.insumotronics.model.CartM;
import com.alexander.springboot.insumotronics.model.CreateCartM;
import com.alexander.springboot.insumotronics.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.alexander.springboot.insumotronics.exception.CartNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/cart")
public class CartController {

    @Autowired
    private CartService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    @GetMapping
    public ResponseEntity<Page<CartM>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "creationDate") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/id")
    public ResponseEntity<CartM> getById(@RequestParam UUID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CartNotFoundException("Cart with ID: " + id + ", not found."));
    }

    @GetMapping("/user")
    public ResponseEntity<Page<CartM>> getByUserId(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findByUserId(userId, pageable));
    }

    @GetMapping("/current")
    public ResponseEntity<CartM> getOrCreateCurrentCart(@RequestParam UUID userId) {
        return ResponseEntity.ok(service.getOrCreateByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestPart(value = "sendData") String createDataJson) {
        CreateCartM cartM;

        try{
            cartM = objectMapper.readValue(createDataJson, CreateCartM.class);
        } catch (Exception e){
            throw new RuntimeException("Invalid JSON format: " + e.getMessage());
        }

        // Validación manual usando el validador de Spring
        Set<ConstraintViolation<CreateCartM>> violations = validator.validate(cartM);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<CreateCartM> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        return ResponseEntity.ok(service.create(cartM));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

