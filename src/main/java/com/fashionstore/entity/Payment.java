package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "PAYMENT")
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paymentId")
    private Integer paymentId;

    @Column(name = "orderId")
    private Integer orderId;

    @Column(name = "paymentMethod")
    private String paymentMethod;

    @Column(name = "paymentStatus")
    private String paymentStatus;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "transactionCode")
    private String transactionCode;

    @Column(name = "paidAt")
    private LocalDateTime paidAt;
    
    public String getPaidAtText() {
        return paidAt == null ? "" : paidAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}