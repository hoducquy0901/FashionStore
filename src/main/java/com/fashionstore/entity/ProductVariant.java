package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT_VARIANT")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variantId")
    private Integer variantId;

    @Column(name = "productId")
    private Integer productId;

    @Column(name = "sizeId")
    private Integer sizeId;

    @Column(name = "colorId")
    private Integer colorId;

    @Column(name = "sku")
    private String sku;

    @Column(name = "price")
    private java.math.BigDecimal price;

    @Column(name = "status")
    private String status;

}
