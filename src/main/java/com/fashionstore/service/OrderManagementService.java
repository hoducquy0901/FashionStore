package com.fashionstore.service;

import com.fashionstore.entity.Address;
import com.fashionstore.entity.OrderDetail;
import com.fashionstore.entity.OrderStatusHistory;
import com.fashionstore.entity.Orders;
import com.fashionstore.entity.Payment;
import com.fashionstore.entity.User;
import com.fashionstore.exception.OrderException;
import com.fashionstore.repository.AddressRepository;
import com.fashionstore.repository.InventoryRepository;
import com.fashionstore.repository.OrderDetailRepository;
import com.fashionstore.repository.OrderStatusHistoryRepository;
import com.fashionstore.repository.OrdersRepository;
import com.fashionstore.repository.PaymentRepository;
import com.fashionstore.repository.UserRepository;
import com.fashionstore.util.OrderLabels;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.fashionstore.util.InventoryConstants;

@Service
public class OrderManagementService {

	/** Đơn thanh toán online quá số phút này mà chưa trả tiền sẽ tự hủy */
	public static final int UNPAID_EXPIRY_MINUTES = 30;

	private static final Map<String, List<String>> TRANSITIONS = Map.of("PENDING", List.of("CONFIRMED", "CANCELLED"),
			"CONFIRMED", List.of("PREPARING", "CANCELLED"), "PREPARING", List.of("SHIPPING", "CANCELLED"), "SHIPPING",
			List.of("COMPLETED", "RETURNED"), "COMPLETED", List.of("RETURNED"), "CANCELLED", List.of(), "RETURNED",
			List.of());

	private final OrdersRepository orderRepository;
	private final OrderDetailRepository orderDetailRepository;
	private final OrderStatusHistoryRepository historyRepository;
	private final PaymentRepository paymentRepository;
	private final InventoryRepository inventoryRepository;
	private final AddressRepository addressRepository;
	private final UserRepository userRepository;
	private final InventoryService inventoryService;

	public OrderManagementService(OrdersRepository orderRepository, OrderDetailRepository orderDetailRepository,
			OrderStatusHistoryRepository historyRepository, PaymentRepository paymentRepository,
			InventoryRepository inventoryRepository, AddressRepository addressRepository, UserRepository userRepository,
			InventoryService inventoryService) {
		this.orderRepository = orderRepository;
		this.orderDetailRepository = orderDetailRepository;
		this.historyRepository = historyRepository;
		this.paymentRepository = paymentRepository;
		this.inventoryRepository = inventoryRepository;
		this.addressRepository = addressRepository;
		this.userRepository = userRepository;
		this.inventoryService = inventoryService;
	}

	// ===================== Xem =====================

	@Transactional(readOnly = true)
	public AdminOrderPage search(String status, String keyword, int page, int size) {
		Page<Orders> result = orderRepository.search(status == null ? "" : status.trim(),
				keyword == null ? "" : keyword.trim(), PageRequest.of(Math.max(page, 0), size));

		List<Integer> orderIds = result.getContent().stream().map(Orders::getOrderId).toList();
		Set<Integer> userIds = result.getContent().stream().map(Orders::getUserId).collect(Collectors.toSet());

		Map<Integer, User> customers = userRepository.findAllById(userIds).stream()
				.collect(Collectors.toMap(User::getUserId, Function.identity()));

		Map<Integer, Payment> payments = new HashMap<>();
		if (!orderIds.isEmpty()) {
			for (Payment p : paymentRepository.findByOrderIdIn(orderIds)) {
				Payment old = payments.get(p.getOrderId());
				if (old == null || p.getPaymentId() > old.getPaymentId()) {
					payments.put(p.getOrderId(), p);
				}
			}
		}
		return new AdminOrderPage(result, customers, payments);
	}

	@Transactional(readOnly = true)
	public AdminOrderView getAdminView(String orderCode) {
		Orders order = orderRepository.findByOrderCode(orderCode)
				.orElseThrow(() -> new OrderException("Không tìm thấy đơn hàng"));
		Payment payment = paymentRepository.findFirstByOrderIdOrderByPaymentIdDesc(order.getOrderId()).orElse(null);

		String status = order.getOrderStatus();
		boolean closed = "CANCELLED".equals(status) || "RETURNED".equals(status);
		boolean refundNeeded = payment != null && closed && "SUCCESS".equals(payment.getPaymentStatus());
		boolean canMarkPaid = payment != null && !closed && !"SUCCESS".equals(payment.getPaymentStatus())
				&& !"REFUNDED".equals(payment.getPaymentStatus());

		Address address = addressRepository.findById(order.getAddressId()).orElse(null);
		User customer = userRepository.findById(order.getUserId()).orElse(null);

		return new AdminOrderView(order, orderDetailRepository.findDetailedByOrderId(order.getOrderId()), payment,
				address, customer, historyRepository.findByOrderIdOrderByHistoryIdAsc(order.getOrderId()),
				TRANSITIONS.getOrDefault(status, List.of()), refundNeeded, canMarkPaid);
	}

	// ===================== Admin thao tác =====================

	@Transactional
	public void changeStatus(String orderCode, String newStatus, Integer actorId, String note) {
		Orders order = lockOrder(orderCode);
		String label = OrderLabels.ORDER_STATUS.getOrDefault(newStatus, newStatus);
		applyStatusChange(order, newStatus, actorId, noteOf("Admin chuyển trạng thái: " + label, note));
	}

	/** Admin xác nhận đã nhận tiền (chuyển khoản, COD thu hộ...) */
	@Transactional
	public void markPaid(String orderCode, Integer actorId) {
		Orders order = lockOrder(orderCode);
		Payment payment = lockPayment(order);
		String status = order.getOrderStatus();
		if ("CANCELLED".equals(status) || "RETURNED".equals(status)) {
			throw new OrderException("Đơn hàng đã hủy hoặc đã trả hàng");
		}
		if ("SUCCESS".equals(payment.getPaymentStatus()) || "REFUNDED".equals(payment.getPaymentStatus())) {
			throw new OrderException("Đơn hàng đã được xử lý thanh toán");
		}
		payment.setPaymentStatus("SUCCESS");
		payment.setPaidAt(LocalDateTime.now());
		addHistory(order, status, actorId, "Admin xác nhận đã nhận thanh toán ("
				+ OrderLabels.PAYMENT_METHOD.getOrDefault(payment.getPaymentMethod(), payment.getPaymentMethod())
				+ ")");
	}

	/**
	 * Admin ghi nhận đã hoàn tiền cho khách (việc chuyển tiền làm bên ngoài hệ
	 * thống)
	 */
	@Transactional
	public void markRefunded(String orderCode, Integer actorId) {
		Orders order = lockOrder(orderCode);
		Payment payment = lockPayment(order);
		String status = order.getOrderStatus();
		if (!"CANCELLED".equals(status) && !"RETURNED".equals(status)) {
			throw new OrderException("Chỉ hoàn tiền cho đơn đã hủy hoặc đã trả hàng");
		}
		if (!"SUCCESS".equals(payment.getPaymentStatus())) {
			throw new OrderException("Đơn hàng chưa có khoản thanh toán thành công để hoàn");
		}
		payment.setPaymentStatus("REFUNDED");
		addHistory(order, status, actorId, "Admin ghi nhận đã hoàn tiền cho khách");
	}

	// ===================== Khách hủy đơn =====================

	@Transactional
	public void customerCancel(String email, String orderCode, String reason) {
		Integer userId = userRepository.findByEmail(email.trim().toLowerCase()).map(User::getUserId)
				.orElseThrow(() -> new OrderException("Không tìm thấy tài khoản"));
		Orders order = lockOrder(orderCode);
		if (!userId.equals(order.getUserId())) {
			throw new OrderException("Không tìm thấy đơn hàng");
		}
		if (!"PENDING".equals(order.getOrderStatus())) {
			throw new OrderException("Chỉ hủy được đơn đang chờ xác nhận. Vui lòng liên hệ cửa hàng để được hỗ trợ.");
		}
		applyStatusChange(order, "CANCELLED", userId, noteOf("Khách hủy đơn", reason));
	}

	// ===================== Tự hủy đơn quá hạn =====================

	@Transactional(readOnly = true)
	public List<String> findExpiredUnpaidOrderCodes(LocalDateTime cutoff) {
		return orderRepository.findExpiredUnpaidOnline(cutoff).stream().map(Orders::getOrderCode).toList();
	}

	/**
	 * Hủy từng đơn trong một transaction riêng. Kiểm tra lại điều kiện sau khi khóa
	 * đơn.
	 */
	@Transactional
	public boolean autoCancelIfExpired(String orderCode, LocalDateTime cutoff) {
		Orders order = lockOrder(orderCode);
		if (!"PENDING".equals(order.getOrderStatus()) || !order.getCreateAt().isBefore(cutoff)) {
			return false;
		}
		Payment payment = lockPayment(order);
		if (!PaymentService.METHOD_VNPAY.equals(payment.getPaymentMethod())
				|| "SUCCESS".equals(payment.getPaymentStatus())) {
			return false;
		}
		applyStatusChange(order, "CANCELLED", order.getUserId(),
				"Hệ thống tự hủy do quá " + UNPAID_EXPIRY_MINUTES + " phút chưa thanh toán online");
		return true;
	}

	// ===================== Lõi: đổi trạng thái + tồn kho + thanh toán
	// =====================

	private void applyStatusChange(Orders order, String newStatus, Integer actorId, String note) {
		String current = order.getOrderStatus();
		if (!TRANSITIONS.getOrDefault(current, List.of()).contains(newStatus)) {
			throw new OrderException(
					"Không thể chuyển đơn từ \"" + OrderLabels.ORDER_STATUS.getOrDefault(current, current)
							+ "\" sang \"" + OrderLabels.ORDER_STATUS.getOrDefault(newStatus, newStatus) + "\"");
		}

		List<OrderDetail> details = orderDetailRepository.findDetailedByOrderId(order.getOrderId());
		Payment payment = paymentRepository.findTopByOrderIdOrderByPaymentIdDesc(order.getOrderId()).orElse(null);

		switch (newStatus) {
		case "CANCELLED" -> {
			// Hàng chưa rời kho: chỉ cần trả phần giữ chỗ
			for (OrderDetail d : details) {
				inventoryRepository.release(d.getVariant().getVariantId(), d.getQuantity());
			}
			if (payment != null && "PENDING".equals(payment.getPaymentStatus())) {
				payment.setPaymentStatus("FAILED");
			}
		}
		case "SHIPPING" -> {
			// Hàng rời kho: trừ tồn thật, bỏ giữ chỗ
			for (OrderDetail d : details) {
				Integer vid = d.getVariant().getVariantId();
				if (inventoryRepository.shipOut(vid, d.getQuantity()) == 0) {
					throw new OrderException("Kho không đủ hàng để xuất cho sản phẩm \""
							+ d.getVariant().getProduct().getProductName() + "\"");
				}
				inventoryService.recordMovement(vid, actorId, InventoryConstants.TX_EXPORT, d.getQuantity(),
						"Xuất kho giao đơn " + order.getOrderCode());
			}
		}
		case "RETURNED" -> {
			// Hàng quay lại kho
			for (OrderDetail d : details) {
				Integer vid = d.getVariant().getVariantId();
				inventoryRepository.restock(vid, d.getQuantity());
				inventoryService.recordMovement(vid, actorId, InventoryConstants.TX_RETURN, d.getQuantity(),
						"Hàng trả về kho, đơn " + order.getOrderCode());
			}
		}
		case "COMPLETED" -> {
			// COD: giao thành công nghĩa là đã thu tiền mặt
			if (payment != null && "COD".equals(payment.getPaymentMethod())
					&& !"SUCCESS".equals(payment.getPaymentStatus())) {
				payment.setPaymentStatus("SUCCESS");
				payment.setPaidAt(LocalDateTime.now());
			}
		}
		default -> {
		}
		}

		order.setOrderStatus(newStatus);
		addHistory(order, newStatus, actorId, note);
	}

	private void addHistory(Orders order, String status, Integer actorId, String note) {
		OrderStatusHistory h = new OrderStatusHistory();
		h.setOrderId(order.getOrderId());
		h.setUpdateBy(actorId);
		h.setStatus(status);
		h.setNote(note);
		historyRepository.save(h);
	}

	private Orders lockOrder(String orderCode) {
		return orderRepository.lockByOrderCode(orderCode)
				.orElseThrow(() -> new OrderException("Không tìm thấy đơn hàng"));
	}

	private Payment lockPayment(Orders order) {
		return paymentRepository.findTopByOrderIdOrderByPaymentIdDesc(order.getOrderId())
				.orElseThrow(() -> new OrderException("Đơn hàng chưa có thông tin thanh toán"));
	}

	private String noteOf(String prefix, String extra) {
		String e = extra == null ? "" : extra.trim();
		if (e.isEmpty())
			return prefix;
		return prefix + ": " + (e.length() > 150 ? e.substring(0, 150) : e);
	}

	// ===================== Dữ liệu trả về cho giao diện =====================

	@Getter
	@AllArgsConstructor
	public static class AdminOrderPage {
		private final Page<Orders> page;
		private final Map<Integer, User> customers;
		private final Map<Integer, Payment> payments;
	}

	@Getter
	@AllArgsConstructor
	public static class AdminOrderView {
		private final Orders order;
		private final List<OrderDetail> details;
		private final Payment payment;
		private final Address address;
		private final User customer;
		private final List<OrderStatusHistory> history;
		private final List<String> nextStatuses;
		private final boolean refundNeeded;
		private final boolean canMarkPaid;
	}
}