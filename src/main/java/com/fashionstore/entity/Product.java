package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Product")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "productId")
    private Integer productId;

    @Column(name = "categoryId")
    private Integer categoryId;

    @Column(name = "productName")
    private String productName;

    @Column(name = "description")
    private String description;

    @Column(name = "brand")
    private String brand;

    @Column(name = "basePrice")
    private BigDecimal basePrice;

    @Column(name = "status")
    private String status;

    @Column(name = "discountId")
    private Integer discountId;

    @Column(name = "createAt")      // đổi thành CreatedAt nếu DB ghi vậy
    private LocalDateTime createdAt;

    @Column(name = "updateAt")      // đổi thành UpdatedAt nếu DB ghi vậy
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    /** Ảnh chính, nếu không có thì lấy ảnh đầu tiên */
    public String getMainImageUrl() {
        return images.stream()
                .filter(i -> Boolean.TRUE.equals(i.getMain()))
                .findFirst()
                .or(() -> images.stream().findFirst())
                .map(ProductImage::getImageUrl)
                .orElse(null);
    }
}