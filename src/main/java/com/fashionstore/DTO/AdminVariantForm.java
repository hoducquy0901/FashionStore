package com.fashionstore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminVariantForm {
    @NotNull(message = "Vui lòng chọn kích cỡ")
    private Integer sizeId;

    @NotNull(message = "Vui lòng chọn màu sắc")
    private Integer colorId;

    @NotBlank(message = "SKU không được để trống")
    @Size(max = 50, message = "SKU tối đa 50 ký tự")
    private String sku;

    @NotNull(message = "Vui lòng nhập giá biến thể")
    @DecimalMin(value = "0.00", message = "Giá phải lớn hơn hoặc bằng 0")
    @Digits(integer = 16, fraction = 2, message = "Giá vượt quá giới hạn cho phép")
    private BigDecimal price;

    @NotBlank(message = "Vui lòng chọn trạng thái")
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Trạng thái biến thể không hợp lệ")
    private String status = "ACTIVE";
}
