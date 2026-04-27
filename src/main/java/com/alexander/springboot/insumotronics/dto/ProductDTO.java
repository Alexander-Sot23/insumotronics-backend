package com.alexander.springboot.insumotronics.dto;

import com.alexander.springboot.insumotronics.enums.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private UUID id;

    @NotBlank
    private String name;

    @NotNull
    private float price;

    @NotNull
    private int stock;

    @NotNull
    private ProductCategory category;

    private List<String> pathImages;

    private List<String> pathDocuments;

    private LocalDateTime createdDate;

    private LocalDateTime updateDate;
}
