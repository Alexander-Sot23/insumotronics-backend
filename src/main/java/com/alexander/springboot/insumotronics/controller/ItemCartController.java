package com.alexander.springboot.insumotronics.controller;

/*
 * Este controlador funciona tanto para los usuarios con rol STUDENT, TEACHER y ADMIN
 */

import com.alexander.springboot.insumotronics.model.ItemCartM;
import com.alexander.springboot.insumotronics.model.CreateItemCartM;
import com.alexander.springboot.insumotronics.service.ItemCartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.alexander.springboot.insumotronics.exception.ItemCartNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/student/item-cart")
public class ItemCartController {

    @Autowired
    private ItemCartService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    @GetMapping
    public ResponseEntity<Page<ItemCartM>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "creationDate") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/id")
    public ResponseEntity<ItemCartM> getById(@RequestParam UUID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ItemCartNotFoundException("ItemCart with ID: " + id + ", not found."));
    }

    @GetMapping("/cart")
    public ResponseEntity<Page<ItemCartM>> getByCartId(
            @RequestParam UUID cartId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findByCartId(cartId, pageable));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestPart(value = "sendData") String createDataJson) {
        CreateItemCartM itemCartM;

        try{
            itemCartM = objectMapper.readValue(createDataJson, CreateItemCartM.class);
        } catch (Exception e){
            throw new RuntimeException("Invalid JSON format: " + e.getMessage());
        }

        // Validación manual usando el validador de Spring
        Set<ConstraintViolation<CreateItemCartM>> violations = validator.validate(itemCartM);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<CreateItemCartM> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        return ResponseEntity.ok(service.create(itemCartM));
    }

    @PostMapping("/add")
    public ResponseEntity<ItemCartM> addToCurrentCart(
            @RequestParam UUID userId,
            @RequestParam UUID productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(service.addToCart(userId, productId, quantity));
    }

    @PutMapping("/quantity")
    public ResponseEntity<ItemCartM> updateQuantity(@RequestParam UUID id,
                                                      @RequestParam int quantity) {
        return ResponseEntity.ok(service.updateQuantity(id, quantity));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

