package com.fashionstore.service;

import com.fashionstore.DTO.RegisterRequest;
import com.fashionstore.entity.Role;
import com.fashionstore.entity.User;
import com.fashionstore.exception.EmailAlreadyExistsException;
import com.fashionstore.repository.RoleRepository;
import com.fashionstore.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email này đã được sử dụng");
        }
        Role role = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Chưa có role CUSTOMER trong database"));

        User user = new User(); 
        user.setFullName(req.getFullname().trim());
        user.setEmail(email);
        user.setPhone(req.getPhone().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setStatus("ACTIVE");
        user.setRoleId(role.getRoleId());
        userRepository.save(user);
    }

    /** Spring Security gọi hàm này khi đăng nhập (username = email) */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    	User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));

        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new UsernameNotFoundException("Tài khoản chưa được gán quyền"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_" + role.getRoleName())
                .disabled(!"ACTIVE".equalsIgnoreCase(user.getStatus()))
                .build();
    }
}