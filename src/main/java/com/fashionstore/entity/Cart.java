package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "CART")
@Getter @Setter @NoArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartId")
    private Integer cartId;

    @Column(name = "userId")
    private Integer userId;

    @Column(name = "status")
    private String status;

    @Column(name = "CreatedAt")   // đổi theo tên cột thật (createAt / CreatedAt)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")   // đổi theo tên cột thật (updateAt / UpdatedAt)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}