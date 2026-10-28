package com.fashionstore.repository;

import com.fashionstore.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrdersRepository extends JpaRepository<Orders, Integer> {
    Optional<Orders> findByOrderCode(String orderCode);
    boolean existsByOrderCode(String orderCode);
    List<Orders> findByUserIdOrderByOrderIdDesc(Integer userId);
}