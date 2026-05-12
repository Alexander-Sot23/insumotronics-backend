package com.alexander.springboot.insumotronics.controller.admin;

/*
 * Este controlador solo funciona para los usuarios con el rol ADMIN
 */

import com.alexander.springboot.insumotronics.dto.ProductDTO;
import com.alexander.springboot.insumotronics.enums.FileType;
import com.alexander.springboot.insumotronics.exception.ProductNotFoundException;
import com.alexander.springboot.insumotronics.model.CreateProductM;
import com.alexander.springboot.insumotronics.model.UpdateProductM;
import com.alexander.springboot.insumotronics.service.ProductService;
import com.alexander.springboot.insumotronics.service.ReserveService;
import com.alexander.springboot.insumotronics.service.storage.FileStorageService;
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
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/product")
public class ProductControllerADMIN {

    @Autowired
    private ProductService service;

    @Autowired
    private ReserveService reserveService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Validator validator;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalProducts", service.countTotalProducts());
        stats.put("totalReservations", reserveService.countTotalReservations());
        return ResponseEntity.ok(stats);
    }

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

    @PostMapping
    public ResponseEntity<?> create (@RequestPart(value = "sendData") String createDataJson,
                                     @RequestPart(value = "images", required = false) MultipartFile[] images,
                                     @RequestPart(value = "documents", required = false) MultipartFile[] documents){
        CreateProductM productM;

        try{
            productM = objectMapper.readValue(createDataJson, CreateProductM.class);
        } catch (Exception e){
            throw new RuntimeException("Invalid JSON format: " + e.getMessage());
        }

        List<String> imagesList = productM.imageUrls() != null ? new ArrayList<>(productM.imageUrls()) : new ArrayList<>();
        List<String> documentsList = productM.documentUrls() != null ? new ArrayList<>(productM.documentUrls()) : new ArrayList<>();

        // Validación manual usando el validador de Spring
        Set<ConstraintViolation<CreateProductM>> violations = validator.validate(productM);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<CreateProductM> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        try{
            // Validar TODOS los archivos antes de guardarlos
            Map<String, String> fileValidationErrors = validateAllFiles(images, documents);
            if (!fileValidationErrors.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(fileValidationErrors);
            }

            // Si todos los archivos pasan validacion, los guardarmos
            if(images != null){
                for(MultipartFile image : images){
                    String fileName = fileStorageService.saveFile(image);
                    imagesList.add(fileName);
                }
            }
            if(documents != null){
                for(MultipartFile document : documents){
                    String fileName = fileStorageService.saveFile(document);
                    documentsList.add(fileName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception during the upload: " + e);
        }

        productM = new CreateProductM(
                productM.name(),
                productM.price(),
                productM.stock(),
                productM.category(),
                imagesList,
                documentsList
        );

        ProductDTO created = service.create(productM);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestParam UUID id,
                                    @RequestPart(value = "sendData") String updateDataJson,
                                    @RequestPart(value = "images", required = false) MultipartFile[] images,
                                    @RequestPart(value = "documents", required = false) MultipartFile[] documents){
        UpdateProductM productM;

        try{
            productM = objectMapper.readValue(updateDataJson, UpdateProductM.class);
        } catch (Exception e){
            throw new RuntimeException("Invalid JSON format: " + e.getMessage());
        }

        // Validación manual usando el validador de Spring
        Set<ConstraintViolation<UpdateProductM>> violations = validator.validate(productM);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<UpdateProductM> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        ProductDTO existingProduct = service.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID: " + id + ", not found."));

        List<String> imagesList = productM.imageUrls() != null ? new ArrayList<>(productM.imageUrls()) : new ArrayList<>(existingProduct.getImageUrls() != null ? existingProduct.getImageUrls() : List.of());
        List<String> documentsList = productM.documentUrls() != null ? new ArrayList<>(productM.documentUrls()) : new ArrayList<>(existingProduct.getDocumentUrls() != null ? existingProduct.getDocumentUrls() : List.of());

        try{
            // Validar TODOS los archivos antes de guardarlos
            Map<String, String> fileValidationErrors = validateAllFiles(images, documents);
            if (!fileValidationErrors.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(fileValidationErrors);
            }

            // Si todos los archivos pasan validación, los guardamos
            if(images != null){
                for(MultipartFile image : images){
                    String fileName = fileStorageService.saveFile(image);
                    imagesList.add(fileName);
                }
            }
            if(documents != null){
                for(MultipartFile document : documents){
                    String fileName = fileStorageService.saveFile(document);
                    documentsList.add(fileName);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception during the upload: " + e);
        }

        productM = new UpdateProductM(
                productM.name(),
                productM.price(),
                productM.stock(),
                productM.category(),
                imagesList,
                documentsList
        );

        ProductDTO updated = service.update(id, productM);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@RequestParam UUID id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Valida TODOS los archivos antes de guardarlos para evitar archivos huérfanos
     */
    private Map<String, String> validateAllFiles(MultipartFile[] images, MultipartFile[] documents) {
        Map<String, String> errors = new HashMap<>();

        if (images != null) {
            for (MultipartFile image : images) {
                if (!isValidImageFile(image)) {
                    errors.put("images", "El archivo '" + image.getOriginalFilename() + "' no es una imagen válida. Solo se permiten: PNG, JPG, JPEG");
                    break; // Solo reportar el primer error encontrado
                }
            }
        }

        if (documents != null) {
            for (MultipartFile document : documents) {
                if (!isValidDocumentFile(document)) {
                    errors.put("documents", "El archivo '" + document.getOriginalFilename() + "' no es un PDF válido. Solo se permiten archivos PDF");
                    break; // Solo reportar el primer error encontrado
                }
            }
        }

        return errors;
    }

    /**
     * Valida que el archivo sea una imagen válida (PNG, JPG, JPEG, SVG)
     */
    private boolean isValidImageFile(MultipartFile file){
        FileType fileType = fileStorageService.extractExtension(file.getOriginalFilename());
        return fileType == FileType.PNG ||
                fileType == FileType.JPG ||
                fileType == FileType.JPEG ||
                fileType == FileType.SVG;
    }

    /**
     * Valida que el archivo sea un PDF válido
     */
    private boolean isValidDocumentFile(MultipartFile file){
        FileType fileType = fileStorageService.extractExtension(file.getOriginalFilename());
        return fileType == FileType.PDF;
    }
}
