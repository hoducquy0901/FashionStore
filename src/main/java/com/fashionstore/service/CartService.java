package com.fashionstore.service;

import com.fashionstore.entity.Cart;
import com.fashionstore.entity.CartItem;
import com.fashionstore.entity.ProductVariant;
import com.fashionstore.repository.CartItemRepository;
import com.fashionstore.repository.CartRepository;
import com.fashionstore.repository.ProductVariantRepository;
import com.fashionstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private static final int MAX_QTY = 99;
    private static final String ACTIVE = "ACTIVE";

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductVariantRepository variantRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.variantRepository = variantRepository;
        this.userRepository = userRepository;
    }

    private Integer currentUserId(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(u -> u.getUserId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng"));
    }

    private Cart getOrCreateCart(Integer userId) {
        return cartRepository.findFirstByUserIdAndStatusOrderByCartIdDesc(userId, ACTIVE)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    cart.setStatus(ACTIVE);
                    return cartRepository.save(cart);
                });
    }

    /**
     * Xác định biến thể cần thêm:
     * - có variantId: dùng luôn (nếu còn bán)
     * - không có variantId nhưng sản phẩm chỉ có 1 biến thể: dùng biến thể đó
     * - ngược lại: empty (phải vào trang chi tiết để chọn màu/size)
     */
    @Transactional(readOnly = true)
    public Optional<ProductVariant> resolveVariant(Integer variantId, Integer productId) {
        if (variantId != null) {
            return variantRepository.findById(variantId)
                    .filter(v -> ACTIVE.equalsIgnoreCase(v.getStatus()));
        }
        if (productId != null) {
            List<ProductVariant> list = variantRepository.findByProduct_ProductIdAndStatus(productId, ACTIVE);
            if (list.size() == 1) {
                return Optional.of(list.get(0));
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void addToCart(String email, Integer variantId, int quantity) {
        int qty = Math.max(1, Math.min(quantity, MAX_QTY));
        variantRepository.findById(variantId)
                .filter(v -> ACTIVE.equalsIgnoreCase(v.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại hoặc đã ngừng bán"));

        Cart cart = getOrCreateCart(currentUserId(email));
        CartItem item = cartItemRepository
                .findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId)
                .orElse(null);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setVariant(variantRepository.getReferenceById(variantId));
            item.setQuantity(qty);
        } else {
            item.setQuantity(Math.min(item.getQuantity() + qty, MAX_QTY));
        }
        cartItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getItems(String email) {
        Integer userId = currentUserId(email);
        return cartRepository.findFirstByUserIdAndStatusOrderByCartIdDesc(userId, ACTIVE)
                .map(c -> cartItemRepository.findDetailedByCartId(c.getCartId()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public int countItems(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .flatMap(u -> cartRepository.findFirstByUserIdAndStatusOrderByCartIdDesc(u.getUserId(), ACTIVE))
                .map(c -> cartItemRepository.sumQuantityByCartId(c.getCartId()).intValue())
                .orElse(0);
    }

    @Transactional
    public void updateQuantity(String email, Integer cartItemId, int quantity) {
        CartItem item = findOwnedItem(email, cartItemId);
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(Math.min(quantity, MAX_QTY));
        }
    }

    @Transactional
    public void remove(String email, Integer cartItemId) {
        cartItemRepository.delete(findOwnedItem(email, cartItemId));
    }

    private CartItem findOwnedItem(String email, Integer cartItemId) {
        Integer userId = currentUserId(email);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ"));
        if (!userId.equals(item.getCart().getUserId())) {
            throw new IllegalArgumentException("Bạn không có quyền thao tác giỏ hàng này");
        }
        return item;
    }
}