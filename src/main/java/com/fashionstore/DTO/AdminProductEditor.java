package com.fashionstore.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProductEditor {
    private final AdminProductForm form;
    private final List<AdminVariantRow> variants;
    private final List<AdminImageRow> images;
}
