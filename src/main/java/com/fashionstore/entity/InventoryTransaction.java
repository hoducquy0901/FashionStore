package com.fashionstore.entity;

import com.fashionstore.util.InventoryConstants;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "INVENTORY_TRANSACTION")
@Getter @Setter @NoArgsConstructor
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transactionId")
    private Integer transactionId;

    @Column(name = "inventoryId")
    private Integer inventoryId;

    @Column(name = "userId")
    private Integer userId;

    @Column(name = "transactionType")
    private String transactionType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "note")
    private String note;

    @Column(name = "updateAt")
    private LocalDateTime updateAt;

    @PrePersist
    void onUpdate() {
        if (updateAt == null) updateAt = LocalDateTime.now();
    }

    public String getCreateAtText() {
        return updateAt == null ? "" : updateAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** Số lượng kèm dấu để hiển thị. Cột quantity luôn là số dương, hướng nằm ở loại giao dịch hoặc ghi chú. */
    public String getSignedQuantityText() {
        int q = quantity == null ? 0 : Math.abs(quantity);
        boolean down = InventoryConstants.TX_EXPORT.equals(transactionType)
                || (InventoryConstants.TX_ADJUST.equals(transactionType)
                    && note != null && note.startsWith(InventoryConstants.NOTE_DOWN));
        return (down ? "-" : "+") + q;
    }

    /** Ghi chú bỏ dấu đánh dấu hướng */
    public String getDisplayNote() {
        if (note == null) return "";
        if (note.startsWith(InventoryConstants.NOTE_DOWN)) return note.substring(InventoryConstants.NOTE_DOWN.length());
        if (note.startsWith(InventoryConstants.NOTE_UP)) return note.substring(InventoryConstants.NOTE_UP.length());
        return note;
    }
}