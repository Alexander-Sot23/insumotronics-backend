package com.alexander.springboot.insumotronics.service.impl;

import com.alexander.springboot.insumotronics.dto.ProductDTO;
import com.alexander.springboot.insumotronics.entity.Product;
import com.alexander.springboot.insumotronics.exception.ProductNotFoundException;
import com.alexander.springboot.insumotronics.model.CreateProductM;
import com.alexander.springboot.insumotronics.model.UpdateProductM;
import com.alexander.springboot.insumotronics.repository.ProductRepository;
import com.alexander.springboot.insumotronics.service.ProductService;
import com.alexander.springboot.insumotronics.service.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository repository;

    @Autowired
    private FileStorageService storageService;

    @Override
    public long countTotalProducts() {
        return repository.count();
    }

    @Override
    public Page<ProductDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::convertToDTO);
    }

    @Override
    public Optional<ProductDTO> findById(UUID id) {
        return repository.findById(id).map(this::convertToDTO);
    }

    @Override
    public Page<ProductDTO> findByName(String name, Pageable pageable) {
        return repository.findByNameContainingIgnoreCase(name, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public ProductDTO create(CreateProductM productM) {
        Product product = convertToModel(productM);
        log.info ("Product creado: {}", product.getId());
        return convertToDTO(repository.save(product));
    }

    @Override
    public Page<ProductDTO> saveAll(List<CreateProductM> products, Pageable pageable) {
        var entities = products.stream()
                .map(this::convertToModel)
                .toList();

        var saved = repository.saveAll(entities);
        var dtos = saved.stream()
                .map(this::convertToDTO)
                .toList();

        int total = dtos.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        java.util.List<ProductDTO> content = start <= end ? dtos.subList(start, end) : java.util.List.of();
        log.info("Products creados mediante saveAll: {} total size", products.size());
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public ProductDTO update(UUID id, UpdateProductM productM) {
        Product productDB = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID: " + id + ", not found."));

        List<String> existingImages = productDB.getPathImages() != null ? new ArrayList<>(productDB.getPathImages()) : new ArrayList<>();
        List<String> existingDocuments = productDB.getPathDocuments() != null ? new ArrayList<>(productDB.getPathDocuments()) : new ArrayList<>();

        List<String> newImages = productM.pathImages() != null ? productM.pathImages() : existingImages;
        List<String> newDocuments = productM.pathDocuments() != null ? productM.pathDocuments() : existingDocuments;

        List<String> removedImages = getRemovedFiles(existingImages, newImages);
        List<String> removedDocuments = getRemovedFiles(existingDocuments, newDocuments);

        deleteFiles(removedImages);
        deleteFiles(removedDocuments);

        productDB.setName(productM.name());
        productDB.setPrice(productM.price());
        productDB.setStock(productM.stock());
        productDB.setCategory(productM.category());
        productDB.setPathImages(new ArrayList<>(newImages));
        productDB.setPathDocuments(new ArrayList<>(newDocuments));

        repository.save(productDB);
        return convertToDTO(productDB);
    }

    @Override
    public ProductDTO updateStock(UUID id, int stock) {
        Product productDB = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID: " + id + ", not found."));
        productDB.setStock(stock);
        return convertToDTO(productDB);
    }

    @Override
    public void delete(UUID id) {
        Product productDB = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID: " + id + ", not found."));

        deleteFiles(productDB.getPathImages());
        deleteFiles(productDB.getPathDocuments());

        log.info("Product eliminado: {}", id);

        repository.delete(productDB);
    }

    private void deleteFiles(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return;
        }
        for (String fileName : fileNames) {
            try {
                storageService.deleteFile(fileName);
            } catch (IOException e) {
                throw new RuntimeException("Error deleting file '" + fileName + "'", e);
            }
        }
    }

    private List<String> getRemovedFiles(List<String> existing, List<String> keep) {
        if (existing == null) {
            return new ArrayList<>();
        }
        if (keep == null) {
            return new ArrayList<>();
        }
        return existing.stream()
                .filter(fileName -> !keep.contains(fileName))
                .toList();
    }

    private ProductDTO convertToDTO(Product product){
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getPathImages(),
                product.getPathDocuments(),
                product.getCreatedDate(),
                product.getUpdatedDate()
        );
    }

    private Product convertToModel(CreateProductM productM){
        Product product = new Product();
        product.setName(productM.name());
        product.setPrice(productM.price());
        product.setStock(productM.stock());
        product.setCategory(productM.category());
        product.setPathImages(productM.pathImages() != null ? productM.pathImages() : new ArrayList<>());
        product.setPathDocuments(productM.pathDocuments() != null ? productM.pathDocuments() : new ArrayList<>());
        return product;
    }
}

