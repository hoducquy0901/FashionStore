package com.fashionstore.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OrderLabels {

    private OrderLabels() {}

    public static final Map<String, String> ORDER_STATUS;
    public static final Map<String, String> PAYMENT_METHOD;
    public static final Map<String, String> PAYMENT_STATUS;

    static {
        Map<String, String> s = new LinkedHashMap<>();
        s.put("PENDING", "Chờ xác nhận");
        s.put("CONFIRMED", "Đã xác nhận");
        s.put("PREPARING", "Đang chuẩn bị hàng");
        s.put("SHIPPING", "Đang giao");
        s.put("COMPLETED", "Hoàn thành");
        s.put("CANCELLED", "Đã hủy");
        s.put("RETURNED", "Đã trả hàng");
        ORDER_STATUS = Collections.unmodifiableMap(s);

        Map<String, String> m = new LinkedHashMap<>();
        m.put("COD", "Thanh toán khi nhận hàng (COD)");
        m.put("BANK_TRANSFER", "Chuyển khoản ngân hàng");
        m.put("E_WALLET", "Thanh toán online (VNPay)");
        m.put("CREDIT_CARD", "Thẻ tín dụng");
        PAYMENT_METHOD = Collections.unmodifiableMap(m);

        Map<String, String> p = new LinkedHashMap<>();
        p.put("PENDING", "Chờ thanh toán");
        p.put("SUCCESS", "Đã thanh toán");
        p.put("FAILED", "Chưa thanh toán / thất bại");
        p.put("REFUNDED", "Đã hoàn tiền");
        PAYMENT_STATUS = Collections.unmodifiableMap(p);
    }
}