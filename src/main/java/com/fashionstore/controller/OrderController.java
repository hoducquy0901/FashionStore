package com.fashionstore.controller;

import com.fashionstore.exception.OrderException;
import com.fashionstore.service.OrderService;
import com.fashionstore.service.OrderService.OrderView;
import java.security.Principal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/orders")
public class OrderController {

	private static final Map<String, String> ORDER_STATUS = Map.of("PENDING", "Chờ xác nhận", "CONFIRMED",
			"Đã xác nhận", "PREPARING", "Đang chuẩn bị hàng", "SHIPPING", "Đang giao", "COMPLETED", "Hoàn thành",
			"CANCELLED", "Đã hủy", "RETURNED", "Đã trả hàng");
	private static final Map<String, String> PAYMENT_METHOD = Map.of(
	        "COD", "Thanh toán khi nhận hàng (COD)",
	        "BANK_TRANSFER", "Chuyển khoản ngân hàng",
	        "E_WALLET", "Thanh toán online (VNPay)",
	        "CREDIT_CARD", "Thẻ tín dụng");
	private static final Map<String, String> PAYMENT_STATUS = Map.of("PENDING", "Chờ thanh toán", "SUCCESS",
			"Đã thanh toán", "FAILED", "Thanh toán thất bại", "REFUNDED", "Đã hoàn tiền");

	private final OrderService orderService;

	@Value("${shop.bank.name:Vietcombank}")
	private String bankName;
	@Value("${shop.bank.account:0123456789}")
	private String bankAccount;
	@Value("${shop.bank.owner:CONG TY FASHIONSTORE}")
	private String bankOwner;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public String list(Model model, Principal principal) {
		model.addAttribute("orders", orderService.getOrders(principal.getName()));
		labels(model);
		return "orders";
	}

	@GetMapping("/{orderCode}")
	public String detail(@PathVariable String orderCode, Model model, Principal principal) {
		OrderView view;
		try {
			view = orderService.getOrderView(principal.getName(), orderCode);
		} catch (OrderException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		model.addAttribute("order", view.getOrder());
		model.addAttribute("details", view.getDetails());
		model.addAttribute("payment", view.getPayment());
		model.addAttribute("address", view.getAddress());
		model.addAttribute("bankName", bankName);
		model.addAttribute("bankAccount", bankAccount);
		model.addAttribute("bankOwner", bankOwner);
		labels(model);
		return "order-detail";
	}

	private void labels(Model model) {
		model.addAttribute("orderStatusLabels", ORDER_STATUS);
		model.addAttribute("paymentMethodLabels", PAYMENT_METHOD);
		model.addAttribute("paymentStatusLabels", PAYMENT_STATUS);
	}
}