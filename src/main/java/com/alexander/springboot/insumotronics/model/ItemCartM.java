package com.alexander.springboot.insumotronics.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record ItemCartM(
        UUID id,
        int quantity,
        UUID cartId,
        UUID productId,
        String productName,
        float productPrice,
        LocalDateTime creationDate,
        LocalDateTime updateDate) {
}
