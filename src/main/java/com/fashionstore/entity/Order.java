package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ORDERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderId")
    private Integer orderId;

    @Column(name = "userId")
    private Integer userId;

    @Column(name = "addressId")
    private Integer addressId;

    @Column(name = "orderCode")
    private String orderCode;

    @Column(name = "orderStatus")
    private String orderStatus;

    @Column(name = "totalAmount")
    private java.math.BigDecimal totalAmount;

    @Column(name = "shippingFee")
    private java.math.BigDecimal shippingFee;

    @Column(name = "finalAmount")
    private java.math.BigDecimal finalAmount;

    @Column(name = "createdAt")
    private java.time.LocalDateTime createdAt;

}
