package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "\"ORDER\"")
@Getter @Setter @NoArgsConstructor
public class Orders {
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
    private BigDecimal totalAmount;

    @Column(name = "shippingFee")
    private BigDecimal shippingFee;

    @Column(name = "finalAmount")
    private BigDecimal finalAmount;

    @Column(name = "createAt")  
    private LocalDateTime createAt;

    @PrePersist
    void onCreate() {
        if (createAt == null) createAt = LocalDateTime.now();
    }

    /** Dùng cho giao diện (không lưu DB) */
    public String getCreatedAtText() {
        return createAt == null ? "" : createAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}