package com.alexander.springboot.insumotronics.service;

import com.alexander.springboot.insumotronics.dto.ProductDTO;
import com.alexander.springboot.insumotronics.model.CreateProductM;
import com.alexander.springboot.insumotronics.model.UpdateProductM;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductService {
    long countTotalProducts();
    Page<ProductDTO> findAll(Pageable pageable);
    Optional<ProductDTO> findById(UUID id);
    Page<ProductDTO> findByName(String name, Pageable pageable);
    ProductDTO create(CreateProductM productM);
    Page<ProductDTO> saveAll(List<CreateProductM> products, Pageable pageable);
    ProductDTO update(UUID id, UpdateProductM productM);
    ProductDTO updateStock(UUID id, int stock);
    void delete(UUID id);
}

