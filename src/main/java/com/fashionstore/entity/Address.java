package com.fashionstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ADDRESS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "addressId")
    private Integer addressId;

    @Column(name = "userId")
    private Integer userId;

    @Column(name = "receiverName")
    private String receiverName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "province")
    private String province;

    @Column(name = "district")
    private String district;

    @Column(name = "ward")
    private String ward;

    @Column(name = "addressDetail")
    private String addressDetail;

    @Column(name = "isDefault")
    private Boolean isDefault;

}
