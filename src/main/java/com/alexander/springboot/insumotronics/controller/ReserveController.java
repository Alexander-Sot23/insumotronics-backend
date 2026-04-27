package com.alexander.springboot.insumotronics.controller;

/*
 * Este controlador funciona tanto para los usuarios con rol STUDENT, TEACHER y ADMIN
 */

import com.alexander.springboot.insumotronics.enums.ReserveStatus;
import com.alexander.springboot.insumotronics.model.ReserveM;
import com.alexander.springboot.insumotronics.service.ReserveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/reserve")
public class ReserveController {

    @Autowired
    private ReserveService service;

    @GetMapping
    public ResponseEntity<Page<ReserveM>> getUserHistory(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "creationDate") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findByUserId(userId, pageable));
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Long>> getDashboardStats(@RequestParam UUID userId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalMyReserves", service.countByUserId(userId));
        stats.put("readyToPickUp", service.countByUserIdAndStatus(userId, ReserveStatus.CONFIRMADA));
        stats.put("inSupervision", service.countByUserIdAndStatus(userId, ReserveStatus.SUPERVISION));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ReserveM>> getActiveReserves(@RequestParam UUID userId) {
        return ResponseEntity.ok(service.findActiveReserves(userId));
    }


    @GetMapping("/id")
    public ResponseEntity<ReserveM> getById(@RequestParam UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/status")
    public ResponseEntity<Page<ReserveM>> getByUserIdAndStatus(
            @RequestParam UUID userId,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creationDate") String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findByUserIdAndStatus(userId, status, pageable));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ReserveM> checkoutCart(@RequestParam UUID cartId) {
        return ResponseEntity.ok(service.checkoutCart(cartId));
    }
}
