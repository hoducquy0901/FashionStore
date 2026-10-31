package com.fashionstore.repository;

import com.fashionstore.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Integer> {
    List<InventoryTransaction> findTop100ByInventoryIdOrderByTransactionIdDesc(Integer inventoryId);
}