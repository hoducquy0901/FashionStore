package com.fashionstore.service;

import com.fashionstore.DTO.CheckoutForm;
import com.fashionstore.entity.*;
import com.fashionstore.exception.OrderException;
import com.fashionstore.repository.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    public static final BigDecimal SHIPPING_FEE = new BigDecimal("30000");
    public static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("500000");

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrdersRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryRepository inventoryRepository;

    public OrderService(UserRepository userRepository, CartRepository cartRepository,
                        CartItemRepository cartItemRepository, AddressRepository addressRepository,
                        OrdersRepository orderRepository, OrderDetailRepository orderDetailRepository,
                        OrderStatusHistoryRepository historyRepository, PaymentRepository paymentRepository,
                        InventoryRepository inventoryRepository) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.orderRepository = orderRepository;
        this.orderDetailRepository = orderDetailRepository;
        this.historyRepository = historyRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public BigDecimal shippingFee(BigDecimal total) {
        if (total.signum() == 0 || total.compareTo(FREE_SHIPPING_FROM) >= 0) {
            return BigDecimal.ZERO;
        }
        return SHIPPING_FEE;
    }

    private Integer currentUserId(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(u -> u.getUserId())
                .orElseThrow(() -> new OrderException("Không tìm thấy tài khoản"));
    }

    @Transactional(readOnly = true)
    public List<Address> getAddresses(String email) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescAddressIdDesc(currentUserId(email));
    }

    /** Đặt hàng từ giỏ hàng hiện tại. Lỗi bất kỳ sẽ hoàn tác toàn bộ (kể cả giữ chỗ tồn kho). */
    @Transactional
    public Orders placeOrder(String email, CheckoutForm form) {
        Integer userId = currentUserId(email);

        Cart cart = cartRepository.findFirstByUserIdAndStatusOrderByCartIdDesc(userId, "ACTIVE")
                .orElseThrow(() -> new OrderException("Giỏ hàng đang trống"));
        List<CartItem> items = cartItemRepository.findDetailedByCartId(cart.getCartId());
        if (items.isEmpty()) {
            throw new OrderException("Giỏ hàng đang trống");
        }

        Address address = resolveAddress(userId, form);

        // Kiểm tra còn bán, tính tiền, giữ chỗ tồn kho
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem it : items) {
            ProductVariant v = it.getVariant();
            String name = v.getProduct().getProductName();
            if (!"ACTIVE".equalsIgnoreCase(v.getStatus()) || !"ACTIVE".equalsIgnoreCase(v.getProduct().getStatus())) {
                throw new OrderException("Sản phẩm \"" + name + "\" đã ngừng bán, vui lòng xóa khỏi giỏ hàng");
            }
            if (inventoryRepository.reserve(v.getVariantId(), it.getQuantity()) == 0) {
                throw new OrderException("Sản phẩm \"" + name + "\" không đủ hàng trong kho");
            }
            total = total.add(it.getLineTotal());
        }
        BigDecimal fee = shippingFee(total);

        // Đơn hàng
        Orders order = new Orders();
        order.setUserId(userId);
        order.setAddressId(address.getAddressId());
        order.setOrderCode(generateCode());
        order.setOrderStatus("PENDING");
        order.setTotalAmount(total);
        order.setShippingFee(fee);
        order.setFinalAmount(total.add(fee));
        orderRepository.save(order);

        // Chi tiết đơn (lưu giá tại thời điểm mua)
        for (CartItem it : items) {
            OrderDetail d = new OrderDetail();
            d.setOrderId(order.getOrderId());
            d.setVariant(it.getVariant());
            d.setQuantity(it.getQuantity());
            d.setUnitPrice(it.getVariant().getPrice());
            d.setSubTotal(it.getLineTotal());
            d.setDiscountAmount(BigDecimal.ZERO);
            orderDetailRepository.save(d);
        }

        // Lịch sử trạng thái
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(order.getOrderId());
        h.setUpdateBy(userId);
        h.setStatus("PENDING");
        String note = form.getNote() == null ? "" : form.getNote().trim();
        h.setNote(note.isEmpty() ? "Khách đặt hàng" : "Khách đặt hàng: " + cut(note, 200));
        historyRepository.save(h);

        // Thanh toán
        Payment p = new Payment();
        p.setOrderId(order.getOrderId());
        p.setPaymentMethod(form.getPaymentMethod());
        p.setPaymentStatus("PENDING");
        p.setAmount(order.getFinalAmount());
        paymentRepository.save(p);

     // Chốt giỏ hàng
        cart.setStatus("CONVERTED");
        return order;
    }

    private Address resolveAddress(Integer userId, CheckoutForm f) {
        if (f.getAddressId() != null) {
            Address a = addressRepository.findById(f.getAddressId())
                    .orElseThrow(() -> new OrderException("Địa chỉ không tồn tại"));
            if (!userId.equals(a.getUserId())) {
                throw new OrderException("Địa chỉ không hợp lệ");
            }
            return a;
        }
        Address a = new Address();
        a.setUserId(userId);
        a.setReceiverName(f.getReceiverName().trim());
        a.setPhone(f.getPhone().trim());
        a.setProvince(f.getProvince().trim());
        a.setDistrict(f.getDistrict().trim());
        a.setWard(f.getWard().trim());
        a.setAddressDetail(f.getAddressDetail().trim());
        a.setDefaultAddress(addressRepository.countByUserId(userId) == 0);
        return addressRepository.save(a);
    }

    private String generateCode() {
        String code;
        do {
            code = "FS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                    + ThreadLocalRandom.current().nextInt(100, 1000);
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    private String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    @Transactional(readOnly = true)
    public List<Orders> getOrders(String email) {
        return orderRepository.findByUserIdOrderByOrderIdDesc(currentUserId(email));
    }

    @Transactional(readOnly = true)
    public OrderView getOrderView(String email, String orderCode) {
        Integer userId = currentUserId(email);
        Orders order = orderRepository.findByOrderCode(orderCode)
                .filter(o -> userId.equals(o.getUserId()))
                .orElseThrow(() -> new OrderException("Không tìm thấy đơn hàng"));
        return new OrderView(order,
                orderDetailRepository.findDetailedByOrderId(order.getOrderId()),
                paymentRepository.findFirstByOrderIdOrderByPaymentIdDesc(order.getOrderId()).orElse(null),
                addressRepository.findById(order.getAddressId()).orElse(null));
    }
    @Transactional(readOnly = true)
    public User getUser(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
    }

    @Getter
    @AllArgsConstructor
    public static class OrderView {
        private final Orders order;
        private final List<OrderDetail> details;
        private final Payment payment;
        private final Address address;
    }
}