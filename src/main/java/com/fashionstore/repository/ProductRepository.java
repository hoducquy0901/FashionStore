package com.fashionstore.repository;

import com.fashionstore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findTop8ByStatusOrderByCreatedAtDesc(String status);
    List<Product> findByCategoryIdAndStatus(Integer categoryId, String status);

    @Query("select p from Product p where (:status = '' or p.status = :status) "
            + "and (:keyword = '' or lower(p.productName) like lower(concat('%', :keyword, '%')) "
            + "or lower(coalesce(p.brand, '')) like lower(concat('%', :keyword, '%'))) "
            + "order by p.createdAt desc, p.productId desc")
    Page<Product> searchAdmin(@Param("status") String status, @Param("keyword") String keyword,
                              Pageable pageable);
}
