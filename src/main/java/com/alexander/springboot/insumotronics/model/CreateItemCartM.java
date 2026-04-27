package com.alexander.springboot.insumotronics.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateItemCartM(
        @NotNull
        UUID cartId,

        @NotNull
        UUID productId,

        @Min(1)
        int quantity) {
}

