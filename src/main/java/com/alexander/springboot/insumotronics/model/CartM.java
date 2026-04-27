package com.alexander.springboot.insumotronics.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CartM(
        UUID id,
        UUID myUserId,
        List<ItemCartM> items,
        float totalPrice,
        LocalDateTime creationDate,
        LocalDateTime updateDate) {
}

