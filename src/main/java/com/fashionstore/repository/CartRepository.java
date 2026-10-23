package com.fashionstore.repository;

import com.fashionstore.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findFirstByUserIdAndStatusOrderByCartIdDesc(Integer userId, String status);
}