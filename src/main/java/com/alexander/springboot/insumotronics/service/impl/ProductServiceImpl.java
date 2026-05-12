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
        Product productDB = repository.save(convertToModel(productM));
        log.info("Product creado: {}", productDB.getId());
        return convertToDTO(repository.save(productDB));
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
        List<ProductDTO> content = start <= end ? dtos.subList(start, end) : List.of();
        log.info("Products creados mediante saveAll: {} total", products.size());
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public ProductDTO update(UUID id, UpdateProductM productM) {
        Product productDB = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID: " + id + ", not found."));

        List<String> existingImages = productDB.getImageUrls() != null
                ? new ArrayList<>(productDB.getImageUrls()) : new ArrayList<>();
        List<String> existingDocuments = productDB.getDocumentUrls() != null
                ? new ArrayList<>(productDB.getDocumentUrls()) : new ArrayList<>();

        // Las nuevas listas de URLs vienen del request; si no se envían, conservar las existentes
        List<String> newImages = productM.imageUrls() != null
                ? productM.imageUrls() : existingImages;
        List<String> newDocuments = productM.documentUrls() != null
                ? productM.documentUrls() : existingDocuments;

        // Eliminar del storage los archivos que ya no están en la nueva lista
        List<String> removedImages = getRemovedUrls(existingImages, newImages);
        List<String> removedDocuments = getRemovedUrls(existingDocuments, newDocuments);

        deleteFilesFromStorage(removedImages);
        deleteFilesFromStorage(removedDocuments);

        productDB.setName(productM.name());
        productDB.setPrice(productM.price());
        productDB.setStock(productM.stock());
        productDB.setCategory(productM.category());
        productDB.setImageUrls(new ArrayList<>(newImages));
        productDB.setDocumentUrls(new ArrayList<>(newDocuments));

        repository.save(productDB);
        log.info("Product actualizado: {}", id);
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

        // Eliminar todos los archivos asociados al producto de Supabase Storage
        deleteFilesFromStorage(productDB.getImageUrls());
        deleteFilesFromStorage(productDB.getDocumentUrls());

        repository.delete(productDB);
        log.info("Product eliminado: {}", id);
    }

    /**
     * Elimina archivos del storage dado su URL completa.
     * Las URLs que no corresponden al patrón de Supabase se ignoran (logs de advertencia).
     */
    private void deleteFilesFromStorage(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) return;
        for (String fileUrl : fileUrls) {
            try {
                boolean deleted = storageService.deleteFile(fileUrl);
                if (!deleted) {
                    log.warn("No se pudo eliminar el archivo: {}", fileUrl);
                }
            } catch (IOException e) {
                // No lanzar excepción para no bloquear la operación principal
                log.error("Error al eliminar archivo '{}' del storage: {}", fileUrl, e.getMessage());
            }
        }
    }

    /**
     * Devuelve las URLs que estaban en la lista existente pero ya no están en la nueva.
     */
    private List<String> getRemovedUrls(List<String> existing, List<String> keep) {
        if (existing == null || existing.isEmpty()) return new ArrayList<>();
        if (keep == null) return new ArrayList<>(existing);
        return existing.stream()
                .filter(url -> !keep.contains(url))
                .toList();
    }

    // =========================================================================
    // CONVERSIONES
    // =========================================================================

    private ProductDTO convertToDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.getImageUrls(),
                product.getDocumentUrls(),
                product.getCreatedDate(),
                product.getUpdatedDate()
        );
    }

    private Product convertToModel(CreateProductM productM) {
        Product product = new Product();
        product.setName(productM.name());
        product.setPrice(productM.price());
        product.setStock(productM.stock());
        product.setCategory(productM.category());
        product.setImageUrls(productM.imageUrls() != null ? productM.imageUrls() : new ArrayList<>());
        product.setDocumentUrls(productM.documentUrls() != null ? productM.documentUrls() : new ArrayList<>());
        return product;
    }
}
