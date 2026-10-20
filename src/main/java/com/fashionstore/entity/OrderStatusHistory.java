package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ORDER_STATUS_HISTORY")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historyId")
    private Integer historyId;

    @Column(name = "orderId")
    private Integer orderId;

    @Column(name = "updatedBy")
    private Integer updatedBy;

    @Column(name = "status")
    private String status;

    @Column(name = "note")
    private String note;

    @Column(name = "createdAt")
    private java.time.LocalDateTime createdAt;

}
