package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "DISCOUNT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Discount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discountId")
    private Integer discountId;

    @Column(name = "discountName")
    private String discountName;

    @Column(name = "discountType")
    private String discountType;

    @Column(name = "discountValue")
    private java.math.BigDecimal discountValue;

    @Column(name = "maxDiscountAmount")
    private java.math.BigDecimal maxDiscountAmount;

    @Column(name = "startDate")
    private java.time.LocalDateTime startDate;

    @Column(name = "endDate")
    private java.time.LocalDateTime endDate;

    @Column(name = "status")
    private String status;

    @Column(name = "createdBy")
    private Integer createdBy;

    @Column(name = "createdAt")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updatedBy")
    private Integer updatedBy;

    @Column(name = "updatedAt")
    private java.time.LocalDateTime updatedAt;

}
