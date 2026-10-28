package com.fashionstore.repository;

import com.fashionstore.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByUserIdOrderByDefaultAddressDescAddressIdDesc(Integer userId);
    long countByUserId(Integer userId);
}