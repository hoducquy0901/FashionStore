package com.fashionstore.service;

import jakarta.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

/** Mã dùng một lần để chống gửi lặp form (bấm Back rồi gửi lại, bấm đúp...) */
@Component
public class FormTokenService {

    private static final String ATTR = "FORM_TOKENS";
    private static final int MAX_TOKENS = 20;   // giữ tối đa 20 mã chưa dùng (nhiều tab)

    @SuppressWarnings("unchecked")
    public String issue(HttpSession session) {
        String token = UUID.randomUUID().toString();
        synchronized (WebUtils.getSessionMutex(session)) {
            Set<String> set = (Set<String>) session.getAttribute(ATTR);
            if (set == null) {
                set = new LinkedHashSet<>();
            }
            if (set.size() >= MAX_TOKENS) {
                Iterator<String> it = set.iterator();
                it.next();
                it.remove();   // bỏ mã cũ nhất
            }
            set.add(token);
            session.setAttribute(ATTR, set);
        }
        return token;
    }

    /** Trả về true đúng một lần cho mỗi mã hợp lệ */
    @SuppressWarnings("unchecked")
    public boolean consume(HttpSession session, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        synchronized (WebUtils.getSessionMutex(session)) {
            Set<String> set = (Set<String>) session.getAttribute(ATTR);
            boolean ok = set != null && set.remove(token);
            if (ok) {
                session.setAttribute(ATTR, set);
            }
            return ok;
        }
    }
}