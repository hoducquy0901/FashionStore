package com.fashionstore.controller;

import com.fashionstore.exception.OrderException;
import com.fashionstore.repository.UserRepository;
import com.fashionstore.service.OrderPackingPdfService;
import com.fashionstore.service.OrderPackingService;
import java.nio.file.Path;
import java.security.Principal;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/admin/packing")
public class AdminPackingController {
    private final OrderPackingService packingService;
    private final OrderPackingPdfService pdfService;
    private final UserRepository userRepository;

    public AdminPackingController(OrderPackingService packingService, OrderPackingPdfService pdfService,
            UserRepository userRepository) {
        this.packingService = packingService;
        this.pdfService = pdfService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("packingOrders", packingService.getQueue());
        return "admin/packing";
    }

    @PostMapping("/scan")
    public String scan(@RequestParam String sku, Principal principal, RedirectAttributes redirect) {
        try {
            var result = packingService.scan(sku, actorId(principal));
            if (result.isComplete()) {
                redirect.addFlashAttribute("packingMessage",
                        "Hoàn thành. Vui lòng mang đến khu vực bàn giao hàng.");
            } else {
                redirect.addFlashAttribute("packingMessage", "Đã nhận " + result.getSku() + " ("
                        + result.getScannedQuantity() + "/" + result.getTargetQuantity() + ") cho đơn "
                        + result.getOrderCode());
            }
        } catch (OrderException e) {
            redirect.addFlashAttribute("packingError", e.getMessage());
        }
        return "redirect:/admin/packing";
    }

    @PostMapping("/orders/{orderCode}/skip")
    public String skip(@PathVariable String orderCode, Principal principal, RedirectAttributes redirect) {
        try {
            packingService.skip(orderCode, actorId(principal));
            redirect.addFlashAttribute("packingMessage", "Đơn " + orderCode + " đã chuyển sang Đang xử lý lỗi");
        } catch (OrderException e) {
            redirect.addFlashAttribute("packingError", e.getMessage());
        }
        return "redirect:/admin/packing";
    }

    @GetMapping("/orders/{orderCode}/pdf")
    public ResponseEntity<FileSystemResource> pdf(@PathVariable String orderCode) {
        Path path;
        try {
            path = pdfService.find(orderCode);
        } catch (OrderException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(path.getFileName().toString()).build().toString())
                .body(new FileSystemResource(path));
    }

    private Integer actorId(Principal principal) {
        return userRepository.findByEmail(principal.getName().trim().toLowerCase())
                .map(user -> user.getUserId())
                .orElseThrow(() -> new OrderException("Không tìm thấy tài khoản quản trị"));
    }
}
