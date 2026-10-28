package com.fashionstore.service;

import com.fashionstore.config.VnpayProperties;
import com.fashionstore.exception.OrderException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class VnpayService {

    private final VnpayProperties props;

    public VnpayService(VnpayProperties props) {
        this.props = props;
    }

    /** Tạo URL chuyển khách sang VNPay. Mỗi lần gọi dùng một mã giao dịch (vnp_TxnRef) mới. */
    public String buildPaymentUrl(String orderCode, BigDecimal amount, String ipAddr) {
        if (isBlank(props.getTmnCode()) || isBlank(props.getHashSecret())) {
            throw new OrderException("Chưa cấu hình VNPay (vnpay.tmn-code, vnpay.hash-secret)");
        }
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        long vnpAmount = amount.setScale(0, RoundingMode.HALF_UP).longValue() * 100;

        Map<String, String> p = new HashMap<>();
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "pay");
        p.put("vnp_TmnCode", props.getTmnCode());
        p.put("vnp_Amount", String.valueOf(vnpAmount));
        p.put("vnp_CurrCode", "VND");
        p.put("vnp_TxnRef", orderCode + "-" + System.currentTimeMillis());
        p.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);
        p.put("vnp_OrderType", "other");
        p.put("vnp_Locale", "vn");
        p.put("vnp_ReturnUrl", props.getReturnUrl());
        p.put("vnp_IpAddr", ipAddr);
        p.put("vnp_CreateDate", now.format(fmt));
        p.put("vnp_ExpireDate", now.plusMinutes(15).format(fmt));

        List<String> names = new ArrayList<>(p.keySet());
        Collections.sort(names);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String name : names) {
            String value = p.get(name);
            if (value == null || value.isEmpty()) continue;
            if (hashData.length() > 0) {
                hashData.append('&');
                query.append('&');
            }
            String encoded = URLEncoder.encode(value, StandardCharsets.US_ASCII);
            hashData.append(name).append('=').append(encoded);
            query.append(URLEncoder.encode(name, StandardCharsets.US_ASCII)).append('=').append(encoded);
        }
        String secureHash = hmacSHA512(props.getHashSecret(), hashData.toString());
        return props.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    /** Kiểm tra chữ ký dữ liệu VNPay gửi về (return URL và IPN). */
    public boolean verifySignature(Map<String, String> params) {
        Map<String, String> fields = new HashMap<>(params);
        String received = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        if (received == null || isBlank(props.getHashSecret())) return false;

        List<String> names = new ArrayList<>(fields.keySet());
        Collections.sort(names);
        StringBuilder hashData = new StringBuilder();
        for (String name : names) {
            String value = fields.get(name);
            if (value == null || value.isEmpty()) continue;
            if (hashData.length() > 0) hashData.append('&');
            hashData.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
        }
        String calculated = hmacSHA512(props.getHashSecret(), hashData.toString());
        return MessageDigest.isEqual(
                calculated.toLowerCase().getBytes(StandardCharsets.UTF_8),
                received.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPay", e);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}