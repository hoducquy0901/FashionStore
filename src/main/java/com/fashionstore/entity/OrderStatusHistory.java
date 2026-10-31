package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "ORDER_STATUS_HISTORY")
@Getter @Setter @NoArgsConstructor
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historyId")
    private Integer historyId;

    @Column(name = "orderId")
    private Integer orderId;

    @Column(name = "updateBy")
    private Integer updateBy;

    @Column(name = "status")
    private String status;

    @Column(name = "note")
    private String note;

    @Column(name = "createAt")
    private LocalDateTime createAt;

    @PrePersist
    void onCreate() {
        if (createAt == null) createAt = LocalDateTime.now();
    }
    public String getCreatedAtText() {
        return createAt == null ? "" : createAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}