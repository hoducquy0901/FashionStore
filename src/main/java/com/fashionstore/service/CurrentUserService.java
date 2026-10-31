package com.fashionstore.service;

import com.fashionstore.exception.InventoryException;
import com.fashionstore.repository.UserRepository;
import java.security.Principal;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Integer requireUserId(Principal principal) {
        return userRepository.findByEmail(principal.getName().trim().toLowerCase())
                .map(u -> u.getUserId())
                .orElseThrow(() -> new InventoryException("Không tìm thấy tài khoản đang đăng nhập"));
    }
}