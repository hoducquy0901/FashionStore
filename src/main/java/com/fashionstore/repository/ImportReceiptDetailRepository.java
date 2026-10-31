package com.fashionstore.repository;

import com.fashionstore.entity.ImportReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ImportReceiptDetailRepository extends JpaRepository<ImportReceiptDetail, Integer> {

    @Query("select d from ImportReceiptDetail d join fetch d.variant v join fetch v.product "
         + "left join fetch v.color left join fetch v.size "
         + "where d.importReceiptId = :receiptId order by d.importDetailId")
    List<ImportReceiptDetail> findDetailedByReceiptId(@Param("receiptId") Integer receiptId);
}