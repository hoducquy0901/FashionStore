package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ORDER_DETAIL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderDetailId")
    private Integer orderDetailId;

    @Column(name = "orderId")
    private Integer orderId;

    @Column(name = "variantId")
    private Integer variantId;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unitPrice")
    private java.math.BigDecimal unitPrice;

    @Column(name = "subTotal")
    private java.math.BigDecimal subTotal;

    @Column(name = "discountAmount")
    private java.math.BigDecimal discountAmount;

}
