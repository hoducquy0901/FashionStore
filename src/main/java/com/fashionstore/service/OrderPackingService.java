package com.fashionstore.service;

import com.fashionstore.entity.OrderDetail;
import com.fashionstore.entity.OrderStatusHistory;
import com.fashionstore.entity.Orders;
import com.fashionstore.entity.Payment;
import com.fashionstore.exception.OrderException;
import com.fashionstore.repository.OrderDetailRepository;
import com.fashionstore.repository.OrderStatusHistoryRepository;
import com.fashionstore.repository.OrdersRepository;
import com.fashionstore.repository.PaymentRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPackingService {
    private static final String SCAN_PREFIX = "PACKING_SCAN:";
    private static final DateTimeFormatter QUEUE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrdersRepository ordersRepository;
    private final OrderDetailRepository detailRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final OrderManagementService orderManagementService;
    private final OrderPackingPdfService pdfService;

    public OrderPackingService(OrdersRepository ordersRepository, OrderDetailRepository detailRepository,
            OrderStatusHistoryRepository historyRepository, PaymentRepository paymentRepository,
            OrderManagementService orderManagementService, OrderPackingPdfService pdfService) {
        this.ordersRepository = ordersRepository;
        this.detailRepository = detailRepository;
        this.historyRepository = historyRepository;
        this.paymentRepository = paymentRepository;
        this.orderManagementService = orderManagementService;
        this.pdfService = pdfService;
    }

    @Transactional(readOnly = true)
    public List<PackingOrder> getQueue() {
        List<PackingOrder> queue = new ArrayList<>();
        for (Orders order : ordersRepository.findOrdersReadyForPacking()) {
            Payment payment = paymentRepository.findFirstByOrderIdOrderByPaymentIdDesc(order.getOrderId())
                    .orElseThrow(() -> new OrderException("Đơn hàng chưa có thông tin thanh toán"));
            LocalDateTime queueAt = queueTime(order, payment);
            queue.add(toView(order, payment, queueAt));
        }
        queue.sort((left, right) -> {
            int timeOrder = left.getQueueAt().compareTo(right.getQueueAt());
            return timeOrder != 0 ? timeOrder
                    : left.getOrder().getOrderId().compareTo(right.getOrder().getOrderId());
        });
        return queue;
    }

    @Transactional
    public ScanResult scan(String rawSku, Integer actorId) {
        String sku = rawSku == null ? "" : rawSku.trim();
        if (sku.isEmpty()) {
            throw new OrderException("Vui lòng nhập hoặc quét mã hàng");
        }
        Orders oldest = getQueue().stream().map(PackingOrder::getOrder).findFirst()
                .orElseThrow(() -> new OrderException("Hiện không có đơn cần đóng hàng"));
        Orders order = ordersRepository.lockByOrderCode(oldest.getOrderCode())
                .orElseThrow(() -> new OrderException("Không tìm thấy đơn hàng"));
        Payment payment = paymentRepository.findTopByOrderIdOrderByPaymentIdDesc(order.getOrderId())
                .orElseThrow(() -> new OrderException("Đơn hàng chưa có thông tin thanh toán"));
        if (!isReadyForPacking(order, payment)) {
            throw new OrderException("Đơn đang xử lý đã thay đổi, vui lòng tải lại danh sách");
        }

        List<OrderDetail> details = detailRepository.findDetailedByOrderId(order.getOrderId());
        Map<Integer, Integer> targetQuantities = targets(details);
        OrderDetail matched = details.stream()
                .filter(detail -> detail.getVariant().getSku() != null
                        && detail.getVariant().getSku().trim().equalsIgnoreCase(sku))
                .findFirst().orElseThrow(() -> new OrderException("Mã " + sku + " không thuộc đơn " + order.getOrderCode()));
        Integer variantId = matched.getVariant().getVariantId();
        Map<Integer, Integer> scanned = scanCounts(order.getOrderId());
        int current = scanned.getOrDefault(variantId, 0);
        int target = targetQuantities.get(variantId);
        if (current >= target) {
            throw new OrderException("Đã quét đủ số lượng mã " + sku + " trong đơn này");
        }

        OrderStatusHistory scanEvent = new OrderStatusHistory();
        scanEvent.setOrderId(order.getOrderId());
        scanEvent.setUpdateBy(actorId);
        scanEvent.setStatus("CONFIRMED");
        scanEvent.setNote(SCAN_PREFIX + variantId + ":" + sku);
        historyRepository.save(scanEvent);
        scanned.put(variantId, current + 1);

        boolean complete = targetQuantities.entrySet().stream()
                .allMatch(entry -> scanned.getOrDefault(entry.getKey(), 0) >= entry.getValue());
        if (complete) {
            orderManagementService.changeStatus(order.getOrderCode(), "PREPARING", actorId,
                    "Đã quét đủ mã hàng, hoàn tất đóng gói");
            pdfService.create(order, details);
        }
        return new ScanResult(order.getOrderCode(), sku, current + 1, target, complete);
    }

    @Transactional
    public void skip(String orderCode, Integer actorId) {
        orderManagementService.changeStatus(orderCode, "TROUBLESHOOTING", actorId,
                "Bỏ qua tại màn hình đóng hàng do phát sinh lỗi nghiệp vụ");
    }

    private PackingOrder toView(Orders order, Payment payment, LocalDateTime queueAt) {
        List<OrderDetail> details = detailRepository.findDetailedByOrderId(order.getOrderId());
        Map<Integer, Integer> targets = targets(details);
        Map<Integer, Integer> scans = scanCounts(order.getOrderId());
        Map<Integer, OrderDetail> representative = details.stream().collect(Collectors.toMap(
                detail -> detail.getVariant().getVariantId(), Function.identity(), (first, ignored) -> first,
                LinkedHashMap::new));
        List<PackingLine> lines = representative.entrySet().stream().map(entry -> {
            OrderDetail detail = entry.getValue();
            var variant = detail.getVariant();
            String options = (variant.getColor() == null ? "" : variant.getColor().getColorName()) + " / "
                    + (variant.getSize() == null ? "" : variant.getSize().getSizeName());
            return new PackingLine(variant.getSku(), variant.getProduct().getProductName(), options,
                    targets.get(entry.getKey()), scans.getOrDefault(entry.getKey(), 0));
        }).toList();
        return new PackingOrder(order, payment, lines, queueAt,
                queueAt == null ? "" : queueAt.format(QUEUE_DATE_FORMAT));
    }

    private LocalDateTime queueTime(Orders order, Payment payment) {
        if (payment.getPaidAt() != null) {
            return payment.getPaidAt();
        }
        return historyRepository.findByOrderIdOrderByHistoryIdAsc(order.getOrderId()).stream()
                .filter(event -> "CONFIRMED".equals(event.getStatus()))
                .filter(event -> event.getNote() == null || !event.getNote().startsWith(SCAN_PREFIX))
                .map(OrderStatusHistory::getCreateAt)
                .filter(time -> time != null)
                .findFirst().orElse(order.getCreateAt());
    }

    private boolean isReadyForPacking(Orders order, Payment payment) {
        return "CONFIRMED".equals(order.getOrderStatus())
                && ("SUCCESS".equals(payment.getPaymentStatus())
                        || ("COD".equals(payment.getPaymentMethod()) && "PENDING".equals(payment.getPaymentStatus())));
    }

    private Map<Integer, Integer> targets(List<OrderDetail> details) {
        return details.stream().collect(Collectors.toMap(detail -> detail.getVariant().getVariantId(),
                OrderDetail::getQuantity, Integer::sum, LinkedHashMap::new));
    }

    private Map<Integer, Integer> scanCounts(Integer orderId) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (OrderStatusHistory event : historyRepository.findByOrderIdOrderByHistoryIdAsc(orderId)) {
            String note = event.getNote();
            if (note == null || !note.startsWith(SCAN_PREFIX)) {
                continue;
            }
            String[] parts = note.substring(SCAN_PREFIX.length()).split(":", 2);
            if (parts.length == 2) {
                try {
                    counts.merge(Integer.valueOf(parts[0]), 1, Integer::sum);
                } catch (NumberFormatException ignored) {
                    // Bỏ qua ghi chú không đúng định dạng, không làm hỏng trang đóng hàng.
                }
            }
        }
        return counts;
    }

    @Getter
    @AllArgsConstructor
    public static class PackingOrder {
        private final Orders order;
        private final Payment payment;
        private final List<PackingLine> lines;
        private final LocalDateTime queueAt;
        private final String queueAtText;
    }

    @Getter
    @AllArgsConstructor
    public static class PackingLine {
        private final String sku;
        private final String productName;
        private final String options;
        private final Integer targetQuantity;
        private final Integer scannedQuantity;
    }

    @Getter
    @AllArgsConstructor
    public static class ScanResult {
        private final String orderCode;
        private final String sku;
        private final Integer scannedQuantity;
        private final Integer targetQuantity;
        private final boolean complete;
    }
}
