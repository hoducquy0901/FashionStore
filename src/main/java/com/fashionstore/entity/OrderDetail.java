package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "ORDER_DETAIL")
@Getter @Setter @NoArgsConstructor
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderDetailId")
    private Integer orderDetailId;

    @Column(name = "orderId")
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variantId")
    private ProductVariant variant;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unitPrice")
    private BigDecimal unitPrice;

    @Column(name = "subTotal")
    private BigDecimal subTotal;

    @Column(name = "DiscountAmount")
    private BigDecimal discountAmount;
}