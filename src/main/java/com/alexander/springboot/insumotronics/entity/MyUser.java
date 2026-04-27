package com.alexander.springboot.insumotronics.entity;

import com.alexander.springboot.insumotronics.enums.AcademicLevel;
import com.alexander.springboot.insumotronics.enums.MyUserStatus;
import com.alexander.springboot.insumotronics.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "my_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String name;

    @NotBlank
    private String lastname;

    @NotBlank
    @Size(min = 6, max = 15)
    @Column(unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_level")
    private AcademicLevel academicLevel;

    @NotBlank
    private String career;

    @NotNull
    private Integer degree;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.STUDENT;

    @Enumerated(EnumType.STRING)
    private MyUserStatus status = MyUserStatus.ACTIVE;

    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    private String passwordHash;

    @Column(name = "first_login")
    private LocalDateTime firstLogin;

    @OneToMany(mappedBy = "myUser", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Cart> carts;

    @OneToMany(mappedBy = "myUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reserve> reserves;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @PrePersist
    protected void onPersist(){
        createdDate = LocalDateTime.now();
        updateDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updateDate = LocalDateTime.now();
    }
}

