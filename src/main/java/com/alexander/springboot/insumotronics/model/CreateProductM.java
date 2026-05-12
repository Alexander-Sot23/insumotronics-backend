package com.alexander.springboot.insumotronics.model;

import com.alexander.springboot.insumotronics.enums.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateProductM(
        @NotBlank
        @Size(min = 1, max = 50)
        String name,

        @NotNull
        float price,

        @NotNull
        int stock,

        @NotNull
        ProductCategory category,

        List<String> imageUrls,

        List<String> documentUrls) {
}
