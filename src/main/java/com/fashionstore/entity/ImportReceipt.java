package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "IMPORT_RECEIPT")
@Getter @Setter @NoArgsConstructor
public class ImportReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "importReceiptId")
    private Integer importReceiptId;

    @Column(name = "receiptCode")
    private String receiptCode;

    @Column(name = "createBy")
    private Integer createBy;

    @Column(name = "status")
    private String status;

    @Column(name = "totalAmount")
    private BigDecimal totalAmount;

    @Column(name = "note")
    private String note;

    @Column(name = "createAt")
    private LocalDateTime createAt;

    @Column(name = "confirmAt")
    private LocalDateTime confirmAt;

    @PrePersist
    void onCreate() {
        if (createAt == null) createAt = LocalDateTime.now();
    }

    public String getCreateAtText() {
        return createAt == null ? "" : createAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getConfirmAtText() {
        return confirmAt == null ? "" : confirmAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}