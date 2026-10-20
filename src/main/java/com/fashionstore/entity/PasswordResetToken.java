package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PASSWORD_RESET_TOKEN")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tokenId")
    private Long tokenId;

    @Column(name = "userId")
    private Integer userId;

    @Column(name = "token")
    private String token;

    @Column(name = "expiresAt")
    private java.time.LocalDateTime expiresAt;

    @Column(name = "usedAt")
    private java.time.LocalDateTime usedAt;

}
