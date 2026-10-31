package com.fashionstore.controller;

import com.fashionstore.exception.OrderException;
import com.fashionstore.repository.UserRepository;
import com.fashionstore.service.OrderManagementService;
import com.fashionstore.service.OrderManagementService.AdminOrderPage;
import com.fashionstore.service.OrderManagementService.AdminOrderView;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

	private final OrderManagementService managementService;
	private final UserRepository userRepository;

	public AdminOrderController(OrderManagementService managementService, UserRepository userRepository) {
		this.managementService = managementService;
		this.userRepository = userRepository;
	}

	@GetMapping
	public String list(@RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "") String keyword,
			@RequestParam(defaultValue = "0") int page, Model model) {
		AdminOrderPage result = managementService.search(status, keyword, page, 10);
		model.addAttribute("orderPage", result.getPage());
		model.addAttribute("orders", result.getPage().getContent());
		model.addAttribute("customers", result.getCustomers());
		model.addAttribute("payments", result.getPayments());
		model.addAttribute("status", status);
		model.addAttribute("keyword", keyword);
		return "admin/orders";
	}

	@GetMapping("/{orderCode}")
	public String detail(@PathVariable String orderCode, Model model) {
		AdminOrderView v;
		try {
			v = managementService.getAdminView(orderCode);
		} catch (OrderException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		model.addAttribute("order", v.getOrder());
		model.addAttribute("details", v.getDetails());
		model.addAttribute("payment", v.getPayment());
		model.addAttribute("address", v.getAddress());
		model.addAttribute("customer", v.getCustomer());
		model.addAttribute("history", v.getHistory());
		model.addAttribute("nextStatuses", v.getNextStatuses());
		model.addAttribute("refundNeeded", v.isRefundNeeded());
		model.addAttribute("canMarkPaid", v.isCanMarkPaid());
		return "admin/order-detail";
	}

	@PostMapping("/{orderCode}/status")
	public String changeStatus(@PathVariable String orderCode, @RequestParam String newStatus,
			@RequestParam(defaultValue = "") String note, Principal principal, RedirectAttributes redirect) {
		try {
			managementService.changeStatus(orderCode, newStatus, actorId(principal), note);
			redirect.addFlashAttribute("adminMessage", "Đã cập nhật trạng thái đơn hàng");
		} catch (OrderException e) {
			redirect.addFlashAttribute("adminError", e.getMessage());
		}
		return "redirect:/admin/orders/" + orderCode;
	}

	@PostMapping("/{orderCode}/mark-paid")
	public String markPaid(@PathVariable String orderCode, Principal principal, RedirectAttributes redirect) {
		try {
			managementService.markPaid(orderCode, actorId(principal));
			redirect.addFlashAttribute("adminMessage", "Đã xác nhận thanh toán");
		} catch (OrderException e) {
			redirect.addFlashAttribute("adminError", e.getMessage());
		}
		return "redirect:/admin/orders/" + orderCode;
	}

	@PostMapping("/{orderCode}/refund")
	public String refund(@PathVariable String orderCode, Principal principal, RedirectAttributes redirect) {
		try {
			managementService.markRefunded(orderCode, actorId(principal));
			redirect.addFlashAttribute("adminMessage", "Đã ghi nhận hoàn tiền");
		} catch (OrderException e) {
			redirect.addFlashAttribute("adminError", e.getMessage());
		}
		return "redirect:/admin/orders/" + orderCode;
	}

	private Integer actorId(Principal principal) {
		return userRepository.findByEmail(principal.getName().trim().toLowerCase()).map(u -> u.getUserId())
				.orElseThrow(() -> new OrderException("Không tìm thấy tài khoản quản trị"));
	}
}