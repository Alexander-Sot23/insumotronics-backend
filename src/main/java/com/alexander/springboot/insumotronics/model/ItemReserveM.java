package com.alexander.springboot.insumotronics.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record ItemReserveM(
        UUID id,
        int quantity,
        float priceU,
        float total,
        UUID reserveId,
        UUID productId,
        String productName,
        LocalDateTime creationDate,
        LocalDateTime updateDate) {
}

