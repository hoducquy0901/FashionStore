package com.fashionstore.repository;

import com.fashionstore.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    List<ProductVariant> findByProduct_ProductIdAndStatus(Integer productId, String status);

	@Query("select v from ProductVariant v join fetch v.product p left join fetch v.color left join fetch v.size "
    	     + "where v.status = 'ACTIVE' order by p.productName, v.variantId")
    	List<ProductVariant> findAllActiveForSelect();
}