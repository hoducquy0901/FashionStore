package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "INVENTORY")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventoryId")
    private Integer inventoryId;

    @Column(name = "variantId")
    private Integer variantId;

    @Column(name = "inventoryLocation")
    private String inventoryLocation;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "reservedQuantity")
    private Integer reservedQuantity;

    @Column(name = "updatedAt")
    private java.time.LocalDateTime updatedAt;

}
