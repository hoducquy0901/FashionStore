package com.fashionstore.controller;

import com.fashionstore.dto.AdminProductEditor;
import com.fashionstore.dto.AdminProductForm;
import com.fashionstore.dto.AdminVariantForm;
import com.fashionstore.service.AdminProductService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private static final Map<String, String> PRODUCT_STATUSES = Map.of(
            "ACTIVE", "Đang bán", "INACTIVE", "Ngừng bán", "DRAFT", "Bản nháp");
    private static final Map<String, String> VARIANT_STATUSES = Map.of(
            "ACTIVE", "Đang bán", "INACTIVE", "Ngừng bán");

    private final AdminProductService productService;

    public AdminProductController(AdminProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        String safeStatus = PRODUCT_STATUSES.containsKey(status) ? status : "";
        Page<?> products = productService.search(safeStatus, keyword, page, 15);
        model.addAttribute("productPage", products);
        model.addAttribute("products", products.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", safeStatus);
        model.addAttribute("statuses", PRODUCT_STATUSES);
        return "admin/products";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        AdminProductForm form = new AdminProductForm();
        form.setBasePrice(BigDecimal.ZERO);
        form.setStatus("ACTIVE");
        prepareForm(model, form, null);
        return "admin/product-form";
    }

    @GetMapping("/{productId}")
    public String editForm(@PathVariable Integer productId, Model model) {
        prepareForm(model, loadForm(productId), productId);
        return "admin/product-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") AdminProductForm form,
                       BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            prepareForm(model, form, form.getProductId());
            return "admin/product-form";
        }
        try {
            Integer productId = productService.saveProduct(form);
            redirect.addFlashAttribute("adminMessage", "Đã lưu thông tin sản phẩm");
            return "redirect:/admin/products/" + productId;
        } catch (IllegalArgumentException exception) {
            model.addAttribute("adminError", exception.getMessage());
            prepareForm(model, form, form.getProductId());
            return "admin/product-form";
        }
    }

    @PostMapping("/{productId}/variants")
    public String addVariant(@PathVariable Integer productId,
                             @Valid @ModelAttribute("variantForm") AdminVariantForm form,
                             BindingResult binding, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("adminError", firstError(binding));
            return "redirect:/admin/products/" + productId + "#variants";
        }
        try {
            productService.saveVariant(productId, null, form);
            redirect.addFlashAttribute("adminMessage", "Đã thêm biến thể sản phẩm");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/products/" + productId + "#variants";
    }

    @PostMapping("/{productId}/variants/{variantId}")
    public String updateVariant(@PathVariable Integer productId, @PathVariable Integer variantId,
                               @Valid @ModelAttribute("variantForm") AdminVariantForm form,
                               BindingResult binding, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("adminError", firstError(binding));
            return "redirect:/admin/products/" + productId + "#variants";
        }
        try {
            productService.saveVariant(productId, variantId, form);
            redirect.addFlashAttribute("adminMessage", "Đã cập nhật biến thể");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/products/" + productId + "#variants";
    }

    @PostMapping("/{productId}/variants/{variantId}/deactivate")
    public String deactivateVariant(@PathVariable Integer productId, @PathVariable Integer variantId,
                                    RedirectAttributes redirect) {
        try {
            productService.deactivateVariant(productId, variantId);
            redirect.addFlashAttribute("adminMessage", "Đã ngừng bán biến thể");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/products/" + productId + "#variants";
    }

    @PostMapping("/{productId}/images")
    public String uploadImage(@PathVariable Integer productId,
                              @RequestParam("file") MultipartFile file,
                              @RequestParam(required = false) Integer variantId,
                              @RequestParam(defaultValue = "false") boolean main,
                              RedirectAttributes redirect) {
        try {
            productService.addImage(productId, variantId, file, main);
            redirect.addFlashAttribute("adminMessage", "Đã tải ảnh lên");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/products/" + productId + "#images";
    }

    @PostMapping("/{productId}/images/{imageId}/main")
    public String setMainImage(@PathVariable Integer productId, @PathVariable Integer imageId,
                               RedirectAttributes redirect) {
        try {
            productService.setMainImage(productId, imageId);
            redirect.addFlashAttribute("adminMessage", "Đã chọn ảnh đại diện sản phẩm");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/products/" + productId + "#images";
    }

    @PostMapping("/{productId}/images/{imageId}/delete")
    public String deleteImage(@PathVariable Integer productId, @PathVariable Integer imageId,
                              RedirectAttributes redirect) {
        try {
            productService.deleteImage(productId, imageId);
            redirect.addFlashAttribute("adminMessage", "Đã xóa ảnh sản phẩm");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirect.addFlashAttribute("adminError", exception.getMessage());
        }
        return "redirect:/admin/products/" + productId + "#images";
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String oversizedUpload() {
        return "error/413";
    }

    private AdminProductForm loadForm(Integer productId) {
        try {
            return productService.getEditor(productId).getForm();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private void prepareForm(Model model, AdminProductForm form, Integer productId) {
        model.addAttribute("form", form);
        model.addAttribute("productStatuses", PRODUCT_STATUSES);
        model.addAttribute("variantStatuses", VARIANT_STATUSES);
        // Keep the shared navbar's `categories` model attribute (Category entities)
        // separate from the lightweight options used by the product form.
        model.addAttribute("categoryOptions", productService.getCategoryOptions());
        model.addAttribute("sizes", productService.getSizeOptions());
        model.addAttribute("colors", productService.getColorOptions());
        if (productId != null) {
            try {
                AdminProductEditor editor = productService.getEditor(productId);
                model.addAttribute("variants", editor.getVariants());
                model.addAttribute("images", editor.getImages());
                AdminVariantForm variantForm = new AdminVariantForm();
                variantForm.setPrice(form.getBasePrice());
                model.addAttribute("variantForm", variantForm);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
    }

    private String firstError(BindingResult binding) {
        return binding.getFieldErrors().stream().findFirst()
                .map(error -> error.getDefaultMessage()).orElse("Dữ liệu biến thể không hợp lệ");
    }
}
