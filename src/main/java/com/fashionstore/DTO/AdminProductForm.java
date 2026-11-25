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
public class AdminProductForm {
    private Integer productId;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Integer categoryId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String productName;

    private String description;

    @Size(max = 100, message = "Thương hiệu tối đa 100 ký tự")
    private String brand;

    @NotNull(message = "Vui lòng nhập giá cơ bản")
    @DecimalMin(value = "0.00", message = "Giá phải lớn hơn hoặc bằng 0")
    @Digits(integer = 16, fraction = 2, message = "Giá vượt quá giới hạn cho phép")
    private BigDecimal basePrice;

    @NotBlank(message = "Vui lòng chọn trạng thái")
    @Pattern(regexp = "ACTIVE|INACTIVE|DRAFT", message = "Trạng thái không hợp lệ")
    private String status = "ACTIVE";
}
