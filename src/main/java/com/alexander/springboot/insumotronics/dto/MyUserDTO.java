package com.alexander.springboot.insumotronics.dto;

import com.alexander.springboot.insumotronics.enums.AcademicLevel;
import com.alexander.springboot.insumotronics.enums.MyUserStatus;
import com.alexander.springboot.insumotronics.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyUserDTO {

    private UUID id;

    @NotBlank
    private String name;

    @NotBlank
    private String lastname;

    @NotBlank
    @Size(min = 6, max = 15)
    private String code;

    @NotNull
    private AcademicLevel academicLevel;

    @NotBlank
    private String career;

    @NotNull
    private Integer degree;

    private UserRole role;

    private MyUserStatus status;

    private String email;

    private LocalDateTime firstLogin;

    private LocalDateTime lastLogin;

    private LocalDateTime createdDate;

    private LocalDateTime updateDate;
}
