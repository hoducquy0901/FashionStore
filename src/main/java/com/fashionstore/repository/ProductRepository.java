package com.fashionstore.repository;

import com.fashionstore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findTop8ByStatusOrderByCreatedAtDesc(String status);
    List<Product> findByCategoryIdAndStatus(Integer categoryId, String status);
}