package com.fashionstore.controller;

import com.fashionstore.entity.ImportReceipt;
import com.fashionstore.exception.InventoryException;
import com.fashionstore.service.CurrentUserService;
import com.fashionstore.service.FormTokenService;
import com.fashionstore.service.ImportService;
import com.fashionstore.service.ImportService.ImportPage;
import com.fashionstore.service.ImportService.ImportView;
import com.fashionstore.util.InventoryConstants;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
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
@RequestMapping("/admin/imports")
public class AdminImportController {

    private final ImportService importService;
    private final CurrentUserService currentUser;
    private final FormTokenService formTokenService;

    public AdminImportController(ImportService importService, CurrentUserService currentUser,
                                 FormTokenService formTokenService) {
        this.importService = importService;
        this.currentUser = currentUser;
        this.formTokenService = formTokenService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        ImportPage result = importService.search(status, keyword, page, 10);
        model.addAttribute("receiptPage", result.getPage());
        model.addAttribute("receipts", result.getPage().getContent());
        model.addAttribute("creators", result.getCreators());
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("importStatusLabels", InventoryConstants.IMPORT_STATUS);
        return "admin/imports";
    }

    @GetMapping("/new")
    public String form(Model model, HttpSession session) {
        model.addAttribute("variants", importService.getVariantOptions());
        model.addAttribute("formToken", formTokenService.issue(session));   // mã dùng một lần
        return "admin/import-form";
    }

    @PostMapping
    public String create(@RequestParam(defaultValue = "") String note,
                         @RequestParam(required = false) List<Integer> variantId,
                         @RequestParam(required = false) List<Integer> quantity,
                         @RequestParam(required = false) List<BigDecimal> unitCost,
                         @RequestParam(defaultValue = "draft") String action,
                         @RequestParam(defaultValue = "") String formToken,
                         HttpSession session, Principal principal, RedirectAttributes redirect) {

        // Mã đã dùng hoặc hết hạn: phiếu này đã được gửi trước đó, không tạo thêm
        if (!formTokenService.consume(session, formToken)) {
            redirect.addFlashAttribute("adminError",
                    "Phiếu nhập này đã được gửi trước đó hoặc trang đã hết hạn nên hệ thống không tạo thêm "
                    + "để tránh nhập trùng. Vui lòng kiểm tra danh sách phiếu nhập trước khi tạo lại.");
            return "redirect:/admin/imports";
        }

        boolean confirmNow = "confirm".equals(action);
        try {
            ImportReceipt r = importService.createReceipt(currentUser.requireUserId(principal),
                    note, variantId, quantity, unitCost, confirmNow);
            redirect.addFlashAttribute("adminMessage",
                    confirmNow ? "Đã tạo phiếu và nhập kho thành công" : "Đã lưu phiếu nhập nháp");
            return "redirect:/admin/imports/" + r.getReceiptCode();
        } catch (InventoryException e) {
            redirect.addFlashAttribute("adminError", e.getMessage());
            return "redirect:/admin/imports/new";
        }
    }

    @GetMapping("/{code}")
    public String detail(@PathVariable String code, Model model) {
        ImportView v;
        try {
            v = importService.getView(code);
        } catch (InventoryException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("receipt", v.getReceipt());
        model.addAttribute("details", v.getDetails());
        model.addAttribute("creator", v.getCreator());
        model.addAttribute("importStatusLabels", InventoryConstants.IMPORT_STATUS);
        return "admin/import-detail";
    }

    @PostMapping("/{code}/confirm")
    public String confirm(@PathVariable String code, Principal principal, RedirectAttributes redirect) {
        try {
            importService.confirm(code, currentUser.requireUserId(principal));
            redirect.addFlashAttribute("adminMessage", "Đã nhập kho thành công");
        } catch (InventoryException e) {
            redirect.addFlashAttribute("adminError", e.getMessage());
        }
        return "redirect:/admin/imports/" + code;
    }

    @PostMapping("/{code}/cancel")
    public String cancel(@PathVariable String code, RedirectAttributes redirect) {
        try {
            importService.cancel(code);
            redirect.addFlashAttribute("adminMessage", "Đã hủy phiếu nhập");
        } catch (InventoryException e) {
            redirect.addFlashAttribute("adminError", e.getMessage());
        }
        return "redirect:/admin/imports/" + code;
    }

    @PostMapping("/{code}/reverse")
    public String reverse(@PathVariable String code, @RequestParam(defaultValue = "") String reason,
                          Principal principal, RedirectAttributes redirect) {
        try {
            importService.reverse(code, currentUser.requireUserId(principal), reason);
            redirect.addFlashAttribute("adminMessage", "Đã hoàn tác phiếu nhập và trừ lại tồn kho");
        } catch (InventoryException e) {
            redirect.addFlashAttribute("adminError", e.getMessage());
        }
        return "redirect:/admin/imports/" + code;
    }
}