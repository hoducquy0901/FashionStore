package com.fashionstore.service;

import com.fashionstore.entity.Inventory;
import com.fashionstore.entity.InventoryTransaction;
import com.fashionstore.entity.User;
import com.fashionstore.exception.InventoryException;
import com.fashionstore.repository.InventoryRepository;
import com.fashionstore.repository.InventoryTransactionRepository;
import com.fashionstore.repository.UserRepository;
import com.fashionstore.util.InventoryConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryTransactionRepository transactionRepository,
                            UserRepository userRepository) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // ===================== Xem =====================

    @Transactional(readOnly = true)
    public InventoryPage search(String keyword, boolean lowOnly, int page, int size) {
        int threshold = InventoryConstants.LOW_STOCK_THRESHOLD;
        Page<Inventory> result = inventoryRepository.search(
                keyword == null ? "" : keyword.trim(), lowOnly, threshold,
                PageRequest.of(Math.max(page, 0), size));
        long outOfStock = inventoryRepository.countAvailableAtMost(0);
        long lowStock = inventoryRepository.countAvailableAtMost(threshold) - outOfStock;
        return new InventoryPage(result, outOfStock, lowStock);
    }

    @Transactional(readOnly = true)
    public InventoryHistory history(Integer inventoryId) {
        Inventory inv = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new InventoryException("Không tìm thấy dòng tồn kho"));
        List<InventoryTransaction> list =
                transactionRepository.findTop100ByInventoryIdOrderByTransactionIdDesc(inventoryId);
        Set<Integer> userIds = list.stream().map(InventoryTransaction::getUserId).collect(Collectors.toSet());
        Map<Integer, String> names = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId,
                        u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail()));
        return new InventoryHistory(inv, list, names);
    }

    // ===================== Ghi =====================

    /** Cộng hàng vào kho (nhập hàng). Chưa có dòng tồn kho thì tự tạo. */
    @Transactional
    public void addStock(Integer variantId, int qty, Integer actorId, String type, String note) {
        Inventory inv = inventoryRepository.lockByVariantId(variantId).orElse(null);
        if (inv == null) {
            inv = new Inventory();
            inv.setVariantId(variantId);
            inv.setInventoryLocation(InventoryConstants.DEFAULT_LOCATION);
            inv.setQuantity(qty);
            inv.setReservedQuantity(0);
            inv.setUpdateAt(LocalDateTime.now());
            inv = inventoryRepository.save(inv);
        } else {
            inv.setQuantity(inv.getQuantity() + qty);
            inv.setUpdateAt(LocalDateTime.now());
        }
        saveTransaction(inv.getInventoryId(), actorId, type, qty, note);
    }

    /** Kiểm kê: đặt lại số lượng thực tế trong kho */
    @Transactional
    public void adjust(Integer inventoryId, Integer newQuantity, String reason, Integer actorId) {
        String r = reason == null ? "" : reason.trim();
        if (r.length() < 3) {
            throw new InventoryException("Vui lòng nhập lý do điều chỉnh (ít nhất 3 ký tự)");
        }
        if (newQuantity == null || newQuantity < 0 || newQuantity > 1_000_000) {
            throw new InventoryException("Số lượng thực tế không hợp lệ");
        }
        Inventory inv = inventoryRepository.lockById(inventoryId)
                .orElseThrow(() -> new InventoryException("Không tìm thấy dòng tồn kho"));
        if (newQuantity < inv.getReservedQuantity()) {
            throw new InventoryException("Số lượng thực tế (" + newQuantity
                    + ") không thể nhỏ hơn số đang giữ chỗ cho đơn hàng (" + inv.getReservedQuantity() + ")");
        }
        int old = inv.getQuantity();
        int delta = newQuantity - old;
        if (delta == 0) {
            throw new InventoryException("Số lượng không thay đổi");
        }
        inv.setQuantity(newQuantity);
        inv.setUpdateAt(LocalDateTime.now());
        int change = Math.abs(delta);
        String direction = delta > 0 ? InventoryConstants.NOTE_UP + "Tăng " : InventoryConstants.NOTE_DOWN + "Giảm ";
        saveTransaction(inv.getInventoryId(), actorId, InventoryConstants.TX_ADJUST, change,
                direction + change + " (từ " + old + " thành " + newQuantity + "): "
                + (r.length() > 150 ? r.substring(0, 150) : r));
    }

    /** Ghi lịch sử cho thao tác kho đã được thực hiện ở nơi khác (giao hàng, trả hàng) */
    @Transactional
    public void recordMovement(Integer variantId, Integer actorId, String type, int qty, String note) {
        inventoryRepository.findByVariantId(variantId)
                .ifPresent(inv -> saveTransaction(inv.getInventoryId(), actorId, type, qty, note));
    }

    private void saveTransaction(Integer inventoryId, Integer actorId, String type, int qty, String note) {
        InventoryTransaction t = new InventoryTransaction();
        t.setInventoryId(inventoryId);
        t.setUserId(actorId);
        t.setTransactionType(type);
        t.setQuantity(qty);
        t.setNote(note != null && note.length() > 250 ? note.substring(0, 250) : note);
        transactionRepository.save(t);
    }
    
    /** Trừ hàng khỏi kho (hoàn tác nhập hàng). Từ chối nếu số có thể bán không đủ. */
    @Transactional
    public void removeStock(Integer variantId, int qty, Integer actorId, String type, String note, String label) {
        Inventory inv = inventoryRepository.lockByVariantId(variantId)
                .orElseThrow(() -> new InventoryException("Không tìm thấy tồn kho của " + label));
        if (inv.getAvailable() < qty) {
            throw new InventoryException("Không thể hoàn tác: " + label + " chỉ còn " + inv.getAvailable()
                    + " có thể bán, ít hơn " + qty + " cần trừ lại (hàng đã bán hoặc đang giữ chỗ cho đơn). "
                    + "Hãy dùng chức năng điều chỉnh tồn kho.");
        }
        inv.setQuantity(inv.getQuantity() - qty);
        inv.setUpdateAt(LocalDateTime.now());
        saveTransaction(inv.getInventoryId(), actorId, type, qty, InventoryConstants.NOTE_DOWN + note);
    }

    // ===================== Dữ liệu trả về cho giao diện =====================

    @Getter
    @AllArgsConstructor
    public static class InventoryPage {
        private final Page<Inventory> page;
        private final long outOfStock;
        private final long lowStock;
    }

    @Getter
    @AllArgsConstructor
    public static class InventoryHistory {
        private final Inventory inventory;
        private final List<InventoryTransaction> transactions;
        private final Map<Integer, String> userNames;
    }
}