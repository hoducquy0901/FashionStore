package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "IMPORT_RECEIPT_DETAIL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportReceiptDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "importDetailId")
    private Integer importDetailId;

    @Column(name = "importReceiptId")
    private Integer importReceiptId;

    @Column(name = "variantId")
    private Integer variantId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unitCost")
    private java.math.BigDecimal unitCost;

    @Column(name = "subTotal")
    private java.math.BigDecimal subTotal;

}
