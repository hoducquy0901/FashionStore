package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYMENT")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private java.math.BigDecimal amount;

    @Column(name = "transactionCode")
    private String transactionCode;

    @Column(name = "paidAt")
    private java.time.LocalDateTime paidAt;

}
