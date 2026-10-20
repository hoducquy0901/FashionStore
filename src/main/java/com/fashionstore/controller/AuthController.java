package com.fashionstore.controller;

import com.fashionstore.DTO.RegisterRequest;
import com.fashionstore.exception.EmailAlreadyExistsException;
import com.fashionstore.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest req,
                           BindingResult result, RedirectAttributes redirect) {

        if (req.getPassword() != null && !req.getPassword().equals(req.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Mật khẩu xác nhận không khớp");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            authService.register(req);
        } catch (EmailAlreadyExistsException e) {
            result.rejectValue("email", "exists", e.getMessage());
            return "register";
        }
        redirect.addFlashAttribute("success", "Đăng ký thành công! Hãy đăng nhập.");
        return "redirect:/login";
    }
}