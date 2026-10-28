package com.fashionstore.controller;

import com.fashionstore.exception.OrderException;
import com.fashionstore.service.PaymentService;
import com.fashionstore.service.PaymentService.VnpayResult;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payment/vnpay")
public class VnpayController {

    private static final Map<String, String> RESPONSE_MESSAGES = Map.of(
            "24", "Bạn đã hủy giao dịch.",
            "11", "Đã hết thời gian chờ thanh toán.",
            "51", "Tài khoản không đủ số dư.",
            "13", "Bạn nhập sai mật khẩu xác thực (OTP).",
            "65", "Tài khoản đã vượt hạn mức giao dịch trong ngày.");

    private final PaymentService paymentService;

    public VnpayController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** Bước 1: tạo URL và chuyển khách sang VNPay (cần đăng nhập) */
    @GetMapping("/pay/{orderCode}")
    public String pay(@PathVariable String orderCode, HttpServletRequest request,
                      Principal principal, RedirectAttributes redirect) {
        try {
            String url = paymentService.createVnpayUrl(principal.getName(), orderCode, clientIp(request));
            return "redirect:" + url;
        } catch (OrderException e) {
            redirect.addFlashAttribute("orderError", e.getMessage());
            return "redirect:/orders/" + orderCode;
        }
    }

    /** Bước 2: khách được VNPay đưa về đây, hiển thị kết quả */
    @GetMapping("/return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        VnpayResult r = paymentService.confirmVnpay(vnpParams(request));

        String status;
        String message;
        String code = request.getParameter("vnp_ResponseCode");
        if (r.getOrderCode() == null || "97".equals(r.getRspCode())
                || "01".equals(r.getRspCode()) || "04".equals(r.getRspCode())) {
            status = "INVALID";
            message = "Không xác thực được kết quả giao dịch. Nếu tài khoản của bạn đã bị trừ tiền, "
                    + "vui lòng liên hệ cửa hàng kèm mã đơn hàng.";
        } else if (r.isSuccess()) {
            status = "SUCCESS";
            message = "Thanh toán thành công. Đơn hàng của bạn đã được xác nhận.";
        } else {
            status = "FAILED";
            message = RESPONSE_MESSAGES.getOrDefault(code, "Giao dịch không thành công.");
        }
        model.addAttribute("status", status);
        model.addAttribute("message", message);
        model.addAttribute("orderCode", r.getOrderCode());
        model.addAttribute("amount", r.getAmount());
        return "payment-result";
    }

    /** Bước 3: VNPay gọi server để thông báo kết quả (IPN) */
    @GetMapping("/ipn")
    @ResponseBody
    public Map<String, String> ipn(HttpServletRequest request) {
        try {
            VnpayResult r = paymentService.confirmVnpay(vnpParams(request));
            return Map.of("RspCode", r.getRspCode(), "Message", r.getMessage());
        } catch (Exception e) {
            return Map.of("RspCode", "99", "Message", "Unknown error");
        }
    }

    private Map<String, String> vnpParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> {
            if (k.startsWith("vnp_") && v.length > 0) {
                params.put(k, v[0]);
            }
        });
        return params;
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        ip = (ip == null || ip.isBlank()) ? request.getRemoteAddr() : ip.split(",")[0].trim();
        return ip.contains(":") ? "127.0.0.1" : ip;   // localhost IPv6 (::1) đổi về IPv4
    }
}