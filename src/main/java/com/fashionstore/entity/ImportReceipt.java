package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "IMPORT_RECEIPT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "importReceiptId")
    private Integer importReceiptId;

    @Column(name = "receiptCode")
    private String receiptCode;

    @Column(name = "createdBy")
    private Integer createdBy;

    @Column(name = "status")
    private String status;

    @Column(name = "totalAmount")
    private java.math.BigDecimal totalAmount;

    @Column(name = "note")
    private String note;

    @Column(name = "createdAt")
    private java.time.LocalDateTime createdAt;

    @Column(name = "confirmedAt")
    private java.time.LocalDateTime confirmedAt;

}
