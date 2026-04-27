package com.alexander.springboot.insumotronics.entity;

import com.alexander.springboot.insumotronics.enums.ProductCategory;
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
@Table(name = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(min = 1, max = 50)
    private String name;

    @NotNull
    private float price;

    @NotNull
    private int stock;

    @Column(name = "product_categoty")
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_path_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "path_image")
    private List<String> pathImages;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_path_documents", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "path_document")
    private List<String> pathDocuments;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ItemCart> itemCarts;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ItemReserve> itemReserves;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "update_date")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onPersist(){
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdated(){
        updatedDate = LocalDateTime.now();
    }
}

