package com.fashionstore.repository;

import com.fashionstore.entity.Color;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Integer> {
    List<Color> findAllByOrderByColorNameAsc();
}
