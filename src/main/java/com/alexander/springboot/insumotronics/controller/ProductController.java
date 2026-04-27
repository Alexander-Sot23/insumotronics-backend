package com.alexander.springboot.insumotronics.controller;

/*
 * Este controlador funciona tanto para los usuarios con rol STUDENT, TEACHER y ADMIN
 */

import com.alexander.springboot.insumotronics.dto.ProductDTO;
import com.alexander.springboot.insumotronics.exception.ProductNotFoundException;
import com.alexander.springboot.insumotronics.service.ProductService;
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
@RequestMapping("/api/student/product")
public class ProductController {

    @Autowired
    private ProductService service;

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdDate") String sort){

        Pageable pageable = PageRequest.of(page,size, Sort.by(sort).descending());
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/id")
    public ResponseEntity<ProductDTO> getById(@RequestParam UUID id){
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID: " + id + ", not found."));
    }

    @GetMapping("/name")
    public ResponseEntity<Page<ProductDTO>> findByName(
            @RequestParam String name,
            Pageable pageable) {
        return ResponseEntity.ok(service.findByName(name, pageable));
    }

}
