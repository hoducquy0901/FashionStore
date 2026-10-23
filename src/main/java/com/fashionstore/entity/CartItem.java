package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "CART_ITEM")
@Getter @Setter @NoArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartItemId")
    private Integer cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartId")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variantId")
    private ProductVariant variant;

    @Column(name = "quantity")
    private Integer quantity;

    /** Thành tiền = đơn giá x số lượng (không lưu DB) */
    public BigDecimal getLineTotal() {
        return variant.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}