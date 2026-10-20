package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CART_ITEM")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartItemId")
    private Integer cartItemId;

    @Column(name = "cartId")
    private Integer cartId;

    @Column(name = "variantId")
    private Integer variantId;

    @Column(name = "quantity")
    private Integer quantity;

}
