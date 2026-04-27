package com.alexander.springboot.insumotronics.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginFormM(@NotBlank
                         String code,

                         @NotBlank
                         @Size(min = 6)
                             /*
                             @Pattern(
                                     regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$",
                                     message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
                             )
                             */
                         String password) {
}
