package com.fashionstore.repository;

import com.fashionstore.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {
    @Query("select od from OrderDetail od join fetch od.variant v join fetch v.product "
         + "where od.orderId = :orderId order by od.orderDetailId")
    List<OrderDetail> findDetailedByOrderId(@Param("orderId") Integer orderId);
}