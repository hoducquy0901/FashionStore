package com.fashionstore.service;

import com.fashionstore.entity.OrderStatusHistory;
import com.fashionstore.entity.Orders;
import com.fashionstore.entity.Payment;
import com.fashionstore.exception.OrderException;
import com.fashionstore.repository.OrderStatusHistoryRepository;
import com.fashionstore.repository.OrdersRepository;
import com.fashionstore.repository.PaymentRepository;
import com.fashionstore.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PaymentService {

    /** VNPay được lưu là E_WALLET vì ràng buộc CK_PAYMENT_Method của DB */
    public static final String METHOD_VNPAY = "E_WALLET";

    private final OrdersRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final VnpayService vnpayService;

    public PaymentService(OrdersRepository orderRepository, PaymentRepository paymentRepository,
                          OrderStatusHistoryRepository historyRepository, UserRepository userRepository,
                          VnpayService vnpayService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.vnpayService = vnpayService;
    }

    /** Kiểm tra đơn hàng hợp lệ để thanh toán, rồi trả về URL chuyển sang VNPay */
    @Transactional
    public String createVnpayUrl(String email, String orderCode, String ipAddr) {
        Integer userId = userRepository.findByEmail(email.trim().toLowerCase())
                .map(u -> u.getUserId())
                .orElseThrow(() -> new OrderException("Không tìm thấy tài khoản"));
        Orders order = orderRepository.findByOrderCode(orderCode)
                .filter(o -> userId.equals(o.getUserId()))
                .orElseThrow(() -> new OrderException("Không tìm thấy đơn hàng"));
        Payment payment = paymentRepository.findFirstByOrderIdOrderByPaymentIdDesc(order.getOrderId())
                .orElseThrow(() -> new OrderException("Đơn hàng chưa có thông tin thanh toán"));

        if (!METHOD_VNPAY.equals(payment.getPaymentMethod())) {
            throw new OrderException("Đơn hàng này không thanh toán qua VNPay");
        }
        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            throw new OrderException("Đơn hàng đã được thanh toán");
        }
        if (!"PENDING".equals(order.getOrderStatus())) {
            throw new OrderException("Đơn hàng không còn ở trạng thái chờ thanh toán");
        }
        payment.setPaymentStatus("PENDING");   // cho phép thanh toán lại sau khi thất bại
        return vnpayService.buildPaymentUrl(order.getOrderCode(), payment.getAmount(), ipAddr);
    }

    /**
     * Xử lý kết quả VNPay gửi về (dùng chung cho IPN và return URL).
     * Có thể được gọi nhiều lần cho cùng một giao dịch mà không bị xử lý trùng.
     */
    @Transactional
    public VnpayResult confirmVnpay(Map<String, String> params) {
        if (!vnpayService.verifySignature(params)) {
            return new VnpayResult("97", "Invalid signature", false, null, null);
        }

        String txnRef = params.get("vnp_TxnRef");
        String orderCode = null;
        if (txnRef != null && !txnRef.isBlank()) {
            int dash = txnRef.indexOf('-');
            orderCode = dash > 0 ? txnRef.substring(0, dash) : txnRef;
        }
        Orders order = orderCode == null ? null : orderRepository.lockByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return new VnpayResult("01", "Order not found", false, orderCode, null);
        }
        Payment payment = paymentRepository.findTopByOrderIdOrderByPaymentIdDesc(order.getOrderId()).orElse(null);
        if (payment == null || !METHOD_VNPAY.equals(payment.getPaymentMethod())) {
            return new VnpayResult("01", "Order not found", false, orderCode, null);
        }

        // So khớp số tiền (VNPay gửi số tiền nhân 100)
        long paidAmount;
        try {
            paidAmount = Long.parseLong(params.get("vnp_Amount"));
        } catch (NumberFormatException e) {
            return new VnpayResult("04", "Invalid amount", false, orderCode, payment.getAmount());
        }
        long expected = payment.getAmount().setScale(0, RoundingMode.HALF_UP).longValue() * 100;
        if (paidAmount != expected) {
            return new VnpayResult("04", "Invalid amount", false, orderCode, payment.getAmount());
        }

        // Đã xác nhận thành công trước đó (IPN và return URL có thể đến lần lượt)
        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            return new VnpayResult("02", "Order already confirmed", true, orderCode, payment.getAmount());
        }

        String responseCode = params.get("vnp_ResponseCode");
        String txnStatus = params.get("vnp_TransactionStatus");
        boolean ok = "00".equals(responseCode) && (txnStatus == null || "00".equals(txnStatus));

        if (ok) {
            String transactionNo = params.get("vnp_TransactionNo");
            payment.setPaymentStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            payment.setTransactionCode(transactionNo);

            if ("PENDING".equals(order.getOrderStatus())) {
                order.setOrderStatus("CONFIRMED");   // đơn đã thanh toán thì tự xác nhận
                OrderStatusHistory h = new OrderStatusHistory();
                h.setOrderId(order.getOrderId());
                h.setUpdateBy(order.getUserId());
                h.setStatus("CONFIRMED");
                h.setNote("Thanh toán VNPay thành công, mã giao dịch " + transactionNo);
                historyRepository.save(h);
            }
        } else {
            payment.setPaymentStatus("FAILED");
        }
        return new VnpayResult("00", "Confirm Success", ok, orderCode, payment.getAmount());
    }

    @Getter
    @AllArgsConstructor
    public static class VnpayResult {
        private final String rspCode;     // mã trả lời cho VNPay (IPN)
        private final String message;
        private final boolean success;    // đơn hàng đã được thanh toán thành công
        private final String orderCode;
        private final BigDecimal amount;
    }
}