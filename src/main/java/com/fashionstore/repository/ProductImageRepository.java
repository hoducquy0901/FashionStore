package com.fashionstore.repository;

import com.fashionstore.entity.ProductImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    List<ProductImage> findByProduct_ProductIdOrderByMainDescImageIdAsc(Integer productId);

    boolean existsByProduct_ProductId(Integer productId);

    @Query("select i from ProductImage i where i.product.productId in :productIds "
            + "order by i.main desc, i.imageId asc")
    List<ProductImage> findForProductIds(@Param("productIds") Collection<Integer> productIds);

    @Modifying
    @Query("update ProductImage i set i.main = false where i.product.productId = :productId")
    int clearMainForProduct(@Param("productId") Integer productId);
}
