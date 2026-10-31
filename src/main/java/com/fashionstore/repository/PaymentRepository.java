package com.fashionstore.repository;

import com.fashionstore.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findFirstByOrderIdOrderByPaymentIdDesc(Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findTopByOrderIdOrderByPaymentIdDesc(Integer orderId);

    List<Payment> findByOrderIdIn(Collection<Integer> orderIds);
}