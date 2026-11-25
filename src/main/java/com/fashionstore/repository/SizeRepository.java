package com.fashionstore.repository;

import com.fashionstore.entity.Size;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, Integer> {
    List<Size> findAllByOrderBySizeNameAsc();
}
