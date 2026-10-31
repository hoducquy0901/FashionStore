package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "INVENTORY")
@Getter @Setter @NoArgsConstructor
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventoryId")
    private Integer inventoryId;

    @Column(name = "variantId")
    private Integer variantId;

    /** Chỉ để đọc thông tin sản phẩm; việc ghi dùng cột variantId ở trên */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variantId", insertable = false, updatable = false)
    private ProductVariant variant;

    @Column(name = "inventoryLocation")
    private String inventoryLocation;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "reservedQuantity")
    private Integer reservedQuantity;

    @Column(name = "updateAt")
    private LocalDateTime updateAt;

    /** Số lượng có thể bán */
    public int getAvailable() {
        return (quantity == null ? 0 : quantity) - (reservedQuantity == null ? 0 : reservedQuantity);
    }

    public String getUpdateAtText() {
        return updateAt == null ? "" : updateAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}