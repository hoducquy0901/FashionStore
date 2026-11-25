package com.fashionstore.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminSelectOption {
    private final Integer id;
    private final String label;
    private final String swatch;
}
