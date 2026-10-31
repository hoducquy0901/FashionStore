package com.fashionstore.service;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);

    private final OrderManagementService managementService;

    public OrderExpiryJob(OrderManagementService managementService) {
        this.managementService = managementService;
    }

    /** Chạy sau khi khởi động 1 phút, rồi cứ 5 phút một lần */
    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void cancelExpiredOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(OrderManagementService.UNPAID_EXPIRY_MINUTES);
        for (String code : managementService.findExpiredUnpaidOrderCodes(cutoff)) {
            try {
                if (managementService.autoCancelIfExpired(code, cutoff)) {
                    log.info("Đã tự hủy đơn quá hạn thanh toán: {}", code);
                }
            } catch (Exception e) {
                log.warn("Không thể tự hủy đơn {}: {}", code, e.getMessage());
            }
        }
    }
}