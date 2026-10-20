package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CART")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cartId")
    private Integer cartId;

    @Column(name = "userId")
    private Integer userId;

    @Column(name = "sessionToken")
    private String sessionToken;

    @Column(name = "status")
    private String status;

    @Column(name = "createdAt")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private java.time.LocalDateTime updatedAt;

}
