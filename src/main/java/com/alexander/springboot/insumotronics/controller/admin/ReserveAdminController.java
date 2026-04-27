package com.alexander.springboot.insumotronics.controller.admin;

/*
 * Este controlador solo funciona para los usuarios con el rol ADMIN
 */

import com.alexander.springboot.insumotronics.model.CancelReserveRequestM;
import com.alexander.springboot.insumotronics.model.ReserveM;
import com.alexander.springboot.insumotronics.service.ReserveService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reserve")
public class ReserveAdminController {

    @Autowired
    private ReserveService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    @GetMapping
    public ResponseEntity<Page<ReserveM>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "creationDate") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/status")
    public ResponseEntity<Page<ReserveM>> getByStatus(
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findByStatus(status, pageable));
    }

    @GetMapping("/id")
    public ResponseEntity<ReserveM> getById(@RequestParam UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PutMapping("/confirm")
    public ResponseEntity<ReserveM> confirmReserve(@RequestParam UUID reserveId) {
        return ResponseEntity.ok(service.confirmReserve(reserveId));
    }

    @PutMapping("/cancel")
    public ResponseEntity<?> cancelReserve(@RequestParam UUID reserveId, @RequestPart(name = "sendData") String createDataJson) {
        CancelReserveRequestM cancelReserveRequestM;

        try{
            cancelReserveRequestM = objectMapper.readValue(createDataJson, CancelReserveRequestM.class);
        } catch (Exception e){
            throw new RuntimeException("Invalid JSON format: " + e.getMessage());
        }

        // Validación manual usando el validador de Spring
        Set<ConstraintViolation<CancelReserveRequestM>> violations = validator.validate(cancelReserveRequestM);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<CancelReserveRequestM> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }
        return ResponseEntity.ok(service.cancelReserve(reserveId, cancelReserveRequestM.message()));
    }
}

