package com.fashionstore.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminVariantRow extends AdminVariantForm {
    private Integer variantId;
    private String sizeName;
    private String colorName;
    private String colorHex;
}
