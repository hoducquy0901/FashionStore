package com.fashionstore.repository;

import com.fashionstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    Optional<CartItem> findByCart_CartIdAndVariant_VariantId(Integer cartId, Integer variantId);

    @Query("select ci from CartItem ci join fetch ci.variant v join fetch v.product "
         + "where ci.cart.cartId = :cartId order by ci.cartItemId")
    List<CartItem> findDetailedByCartId(@Param("cartId") Integer cartId);

    @Query("select coalesce(sum(ci.quantity), 0) from CartItem ci where ci.cart.cartId = :cartId")
    Long sumQuantityByCartId(@Param("cartId") Integer cartId);
}