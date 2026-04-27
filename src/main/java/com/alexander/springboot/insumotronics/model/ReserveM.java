package com.alexander.springboot.insumotronics.model;

import com.alexander.springboot.insumotronics.enums.ReserveStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReserveM(
        UUID id,
        ReserveStatus status,
        float total,
        UUID myUserId,
        List<ItemReserveM> items,
        LocalDateTime creationDate,
        LocalDateTime updateDate) {
}

