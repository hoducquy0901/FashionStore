package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "IMPORT_RECEIPT_DETAIL")
@Getter @Setter @NoArgsConstructor
public class ImportReceiptDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "importDetailId")
    private Integer importDetailId;

    @Column(name = "importReceiptId")
    private Integer importReceiptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variantId")
    private ProductVariant variant;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unitCost")
    private BigDecimal unitCost;

    @Column(name = "subTotal")
    private BigDecimal subTotal;
}