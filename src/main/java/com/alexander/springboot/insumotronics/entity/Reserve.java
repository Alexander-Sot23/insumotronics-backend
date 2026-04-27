package com.alexander.springboot.insumotronics.entity;

import com.alexander.springboot.insumotronics.enums.ReserveStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reserve")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reserve {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reserve_status", length = 20)
    private ReserveStatus status;

    @NotNull
    private float total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "my_user_id", nullable = false)
    private MyUser myUser;

    @OneToMany(mappedBy = "reserve", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemReserve> items;

    @CreationTimestamp
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @PrePersist
    protected void prePersist(){
        creationDate = LocalDateTime.now();
        updateDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void preUpdate(){
        updateDate = LocalDateTime.now();
    }
}

