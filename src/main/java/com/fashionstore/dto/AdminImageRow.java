package com.fashionstore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminImageRow {
    private final Integer imageId;
    private final String imageUrl;
    private final boolean main;
    private final String variantLabel;
}
