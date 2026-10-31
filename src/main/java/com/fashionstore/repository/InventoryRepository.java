package com.fashionstore.repository;

import com.fashionstore.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    // ===== Dùng cho đơn hàng =====

    /** Giữ chỗ khi đặt hàng. Trả về 0 nếu không đủ hàng. */
    @Modifying
    @Query("update Inventory i set i.reservedQuantity = i.reservedQuantity + :qty "
         + "where i.variantId = :variantId and (i.quantity - i.reservedQuantity) >= :qty")
    int reserve(@Param("variantId") Integer variantId, @Param("qty") int qty);

    /** Hủy đơn: trả phần giữ chỗ (không để âm) */
    @Modifying
    @Query("update Inventory i set i.reservedQuantity = "
         + "case when i.reservedQuantity >= :qty then i.reservedQuantity - :qty else 0 end "
         + "where i.variantId = :variantId")
    int release(@Param("variantId") Integer variantId, @Param("qty") int qty);

    /** Giao hàng: trừ tồn thật và bỏ giữ chỗ */
    @Modifying
    @Query("update Inventory i set i.quantity = i.quantity - :qty, i.reservedQuantity = i.reservedQuantity - :qty "
         + "where i.variantId = :variantId and i.reservedQuantity >= :qty and i.quantity >= :qty")
    int shipOut(@Param("variantId") Integer variantId, @Param("qty") int qty);

    /** Trả hàng: hàng quay lại kho */
    @Modifying
    @Query("update Inventory i set i.quantity = i.quantity + :qty where i.variantId = :variantId")
    int restock(@Param("variantId") Integer variantId, @Param("qty") int qty);

    // ===== Dùng cho quản lý kho =====

    Optional<Inventory> findByVariantId(Integer variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.variantId = :variantId")
    Optional<Inventory> lockByVariantId(@Param("variantId") Integer variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.inventoryId = :id")
    Optional<Inventory> lockById(@Param("id") Integer id);

    @Query(value = "select i from Inventory i join fetch i.variant v join fetch v.product p "
            + "where (:keyword = '' or p.productName like concat('%', :keyword, '%') "
            + "or v.sku like concat('%', :keyword, '%')) "
            + "and (:lowOnly = false or (i.quantity - i.reservedQuantity) <= :threshold) "
            + "order by p.productName, v.variantId",
           countQuery = "select count(i) from Inventory i join i.variant v join v.product p "
            + "where (:keyword = '' or p.productName like concat('%', :keyword, '%') "
            + "or v.sku like concat('%', :keyword, '%')) "
            + "and (:lowOnly = false or (i.quantity - i.reservedQuantity) <= :threshold)")
    Page<Inventory> search(@Param("keyword") String keyword, @Param("lowOnly") boolean lowOnly,
                           @Param("threshold") int threshold, Pageable pageable);

    @Query("select count(i) from Inventory i where (i.quantity - i.reservedQuantity) <= :limit")
    long countAvailableAtMost(@Param("limit") int limit);
}