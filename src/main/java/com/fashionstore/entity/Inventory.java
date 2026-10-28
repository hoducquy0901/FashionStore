package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "INVENTORY")
@Getter @Setter @NoArgsConstructor
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventoryId")
    private Integer inventoryId;

    @Column(name = "variantId")
    private Integer variantId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "reservedQuantity")
    private Integer reservedQuantity;
}