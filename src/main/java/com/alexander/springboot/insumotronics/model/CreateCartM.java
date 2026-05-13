package com.alexander.springboot.insumotronics.model;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCartM(
        @NotNull
        UUID myUserId) {
}
