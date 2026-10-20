package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "INVENTORY_TRANSACTION")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transactionId")
    private Integer transactionId;

    @Column(name = "inventoryId")
    private Integer inventoryId;

    @Column(name = "userId")
    private Integer userId;

    @Column(name = "transactionType")
    private String transactionType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "note")
    private String note;

    @Column(name = "updatedAt")
    private java.time.LocalDateTime updatedAt;

}
