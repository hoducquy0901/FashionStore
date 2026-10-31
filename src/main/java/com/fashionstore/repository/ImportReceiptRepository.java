package com.fashionstore.repository;

import com.fashionstore.entity.ImportReceipt;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Integer> {

    Optional<ImportReceipt> findByReceiptCode(String receiptCode);
    boolean existsByReceiptCode(String receiptCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ImportReceipt r where r.receiptCode = :code")
    Optional<ImportReceipt> lockByReceiptCode(@Param("code") String code);

    @Query(value = "select r from ImportReceipt r where (:status = '' or r.status = :status) "
            + "and (:keyword = '' or r.receiptCode like concat('%', :keyword, '%')) order by r.importReceiptId desc",
           countQuery = "select count(r) from ImportReceipt r where (:status = '' or r.status = :status) "
            + "and (:keyword = '' or r.receiptCode like concat('%', :keyword, '%'))")
    Page<ImportReceipt> search(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);
}