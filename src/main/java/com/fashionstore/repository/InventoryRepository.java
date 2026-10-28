package com.fashionstore.repository;

import com.fashionstore.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    /** Trả về 1 nếu giữ chỗ thành công, 0 nếu không đủ hàng hoặc chưa có dòng tồn kho */
    @Modifying
    @Query("update Inventory i set i.reservedQuantity = i.reservedQuantity + :qty "
         + "where i.variantId = :variantId and (i.quantity - i.reservedQuantity) >= :qty")
    int reserve(@Param("variantId") Integer variantId, @Param("qty") int qty);
}