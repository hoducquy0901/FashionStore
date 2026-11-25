package com.fashionstore.repository;

import com.fashionstore.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    List<ProductVariant> findByProduct_ProductIdAndStatus(Integer productId, String status);

	@Query("select v from ProductVariant v join fetch v.product p left join fetch v.color left join fetch v.size "
    	     + "where v.status = 'ACTIVE' order by p.productName, v.variantId")
    	List<ProductVariant> findAllActiveForSelect();

    @Query("select v from ProductVariant v left join fetch v.color left join fetch v.size "
            + "where v.product.productId = :productId order by v.variantId")
    List<ProductVariant> findAllForAdmin(@Param("productId") Integer productId);

    @Query("select v.product.productId, count(v) from ProductVariant v "
            + "where v.product.productId in :productIds group by v.product.productId")
    List<Object[]> countByProductIds(@Param("productIds") Collection<Integer> productIds);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndVariantIdNot(String sku, Integer variantId);

    boolean existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorId(
            Integer productId, Integer sizeId, Integer colorId);

    boolean existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorIdAndVariantIdNot(
            Integer productId, Integer sizeId, Integer colorId, Integer variantId);
}
