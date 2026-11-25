package com.fashionstore.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProductRow {
    private final Integer productId;
    private final String productName;
    private final String brand;
    private final BigDecimal basePrice;
    private final String status;
    private final String categoryName;
    private final String mainImageUrl;
    private final long variantCount;
}
