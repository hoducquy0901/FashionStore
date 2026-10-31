package com.fashionstore.controller;

import com.fashionstore.exception.InventoryException;
import com.fashionstore.service.CurrentUserService;
import com.fashionstore.service.InventoryService;
import com.fashionstore.service.InventoryService.InventoryHistory;
import com.fashionstore.service.InventoryService.InventoryPage;
import com.fashionstore.util.InventoryConstants;
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
@RequestMapping("/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final CurrentUserService currentUser;

    public AdminInventoryController(InventoryService inventoryService, CurrentUserService currentUser) {
        this.inventoryService = inventoryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "false") boolean lowOnly,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        InventoryPage result = inventoryService.search(keyword, lowOnly, page, 15);
        model.addAttribute("inventoryPage", result.getPage());
        model.addAttribute("items", result.getPage().getContent());
        model.addAttribute("outOfStock", result.getOutOfStock());
        model.addAttribute("lowStock", result.getLowStock());
        model.addAttribute("threshold", InventoryConstants.LOW_STOCK_THRESHOLD);
        model.addAttribute("keyword", keyword);
        model.addAttribute("lowOnly", lowOnly);
        return "admin/inventory";
    }

    @PostMapping("/{inventoryId}/adjust")
    public String adjust(@PathVariable Integer inventoryId, @RequestParam Integer quantity,
                         @RequestParam String reason,
                         @RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "false") boolean lowOnly,
                         @RequestParam(defaultValue = "0") int page,
                         Principal principal, RedirectAttributes redirect) {
        try {
            inventoryService.adjust(inventoryId, quantity, reason, currentUser.requireUserId(principal));
            redirect.addFlashAttribute("adminMessage", "Đã điều chỉnh tồn kho");
        } catch (InventoryException e) {
            redirect.addFlashAttribute("adminError", e.getMessage());
        }
        redirect.addAttribute("keyword", keyword);
        redirect.addAttribute("lowOnly", lowOnly);
        redirect.addAttribute("page", page);
        return "redirect:/admin/inventory";
    }

    @GetMapping("/{inventoryId}/history")
    public String history(@PathVariable Integer inventoryId, Model model) {
        InventoryHistory h;
        try {
            h = inventoryService.history(inventoryId);
        } catch (InventoryException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("inventory", h.getInventory());
        model.addAttribute("transactions", h.getTransactions());
        model.addAttribute("userNames", h.getUserNames());
        model.addAttribute("txLabels", InventoryConstants.TX_LABELS);
        return "admin/inventory-history";
    }
}