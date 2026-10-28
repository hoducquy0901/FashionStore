package com.fashionstore.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CheckoutForm {
    private Integer addressId;          // null = nhập địa chỉ mới
    private String receiverName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private String paymentMethod = "COD";
    private String note;
}