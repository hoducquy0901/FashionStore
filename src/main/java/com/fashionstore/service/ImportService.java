package com.fashionstore.service;

import com.fashionstore.entity.ImportReceipt;
import com.fashionstore.entity.ImportReceiptDetail;
import com.fashionstore.entity.ProductVariant;
import com.fashionstore.entity.User;
import com.fashionstore.exception.InventoryException;
import com.fashionstore.repository.ImportReceiptDetailRepository;
import com.fashionstore.repository.ImportReceiptRepository;
import com.fashionstore.repository.ProductVariantRepository;
import com.fashionstore.repository.UserRepository;
import com.fashionstore.util.InventoryConstants;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ImportService {

	private static final int MAX_QTY = 100_000;

	private final ImportReceiptRepository receiptRepository;
	private final ImportReceiptDetailRepository detailRepository;
	private final ProductVariantRepository variantRepository;
	private final UserRepository userRepository;
	private final InventoryService inventoryService;

	public ImportService(ImportReceiptRepository receiptRepository, ImportReceiptDetailRepository detailRepository,
			ProductVariantRepository variantRepository, UserRepository userRepository,
			InventoryService inventoryService) {
		this.receiptRepository = receiptRepository;
		this.detailRepository = detailRepository;
		this.variantRepository = variantRepository;
		this.userRepository = userRepository;
		this.inventoryService = inventoryService;
	}

	// ===================== Xem =====================

	@Transactional(readOnly = true)
	public List<ProductVariant> getVariantOptions() {
		return variantRepository.findAllActiveForSelect();
	}

	@Transactional(readOnly = true)
	public ImportPage search(String status, String keyword, int page, int size) {
		Page<ImportReceipt> result = receiptRepository.search(status == null ? "" : status.trim(),
				keyword == null ? "" : keyword.trim(), PageRequest.of(Math.max(page, 0), size));
		Set<Integer> ids = result.getContent().stream().map(ImportReceipt::getCreateBy).collect(Collectors.toSet());
		Map<Integer, User> creators = userRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(User::getUserId, Function.identity()));
		return new ImportPage(result, creators);
	}

	@Transactional(readOnly = true)
	public ImportView getView(String receiptCode) {
		ImportReceipt receipt = receiptRepository.findByReceiptCode(receiptCode)
				.orElseThrow(() -> new InventoryException("Không tìm thấy phiếu nhập"));
		return new ImportView(receipt, detailRepository.findDetailedByReceiptId(receipt.getImportReceiptId()),
				userRepository.findById(receipt.getCreateBy()).orElse(null));
	}

	// ===================== Tạo, xác nhận, hủy =====================

	@Transactional
	public ImportReceipt createReceipt(Integer actorId, String note, List<Integer> variantIds, List<Integer> quantities,
			List<BigDecimal> unitCosts, boolean confirmNow) {
		if (variantIds == null || variantIds.isEmpty() || quantities == null || unitCosts == null
				|| variantIds.size() != quantities.size() || variantIds.size() != unitCosts.size()) {
			throw new InventoryException("Phiếu nhập cần có ít nhất một sản phẩm");
		}

		ImportReceipt receipt = new ImportReceipt();
		receipt.setReceiptCode(generateCode());
		receipt.setCreateBy(actorId);
		receipt.setStatus(InventoryConstants.IMPORT_DRAFT);
		receipt.setTotalAmount(BigDecimal.ZERO);
		String n = note == null ? "" : note.trim();
		receipt.setNote(n.length() > 500 ? n.substring(0, 500) : n);
		receiptRepository.save(receipt);

		BigDecimal total = BigDecimal.ZERO;
		for (int i = 0; i < variantIds.size(); i++) {
			Integer variantId = variantIds.get(i);
			Integer qty = quantities.get(i);
			BigDecimal cost = unitCosts.get(i);
			if (variantId == null || qty == null || cost == null) {
				throw new InventoryException("Thông tin dòng sản phẩm số " + (i + 1) + " chưa đầy đủ");
			}
			if (qty < 1 || qty > MAX_QTY) {
				throw new InventoryException("Số lượng dòng số " + (i + 1) + " phải từ 1 đến " + MAX_QTY);
			}
			if (cost.signum() < 0) {
				throw new InventoryException("Đơn giá nhập dòng số " + (i + 1) + " không được âm");
			}
			ProductVariant variant = variantRepository.findById(variantId)
					.orElseThrow(() -> new InventoryException("Sản phẩm không tồn tại"));

			ImportReceiptDetail d = new ImportReceiptDetail();
			d.setImportReceiptId(receipt.getImportReceiptId());
			d.setVariant(variant);
			d.setQuantity(qty);
			d.setUnitCost(cost);
			d.setSubTotal(cost.multiply(BigDecimal.valueOf(qty)));
			detailRepository.save(d);
			total = total.add(d.getSubTotal());
		}
		receipt.setTotalAmount(total);

		if (confirmNow) {
			doConfirm(receipt, actorId);
		}
		return receipt;
	}

	@Transactional
	public void confirm(String receiptCode, Integer actorId) {
		ImportReceipt receipt = receiptRepository.lockByReceiptCode(receiptCode)
				.orElseThrow(() -> new InventoryException("Không tìm thấy phiếu nhập"));
		if (!InventoryConstants.IMPORT_DRAFT.equals(receipt.getStatus())) {
			throw new InventoryException("Chỉ xác nhận được phiếu đang ở trạng thái nháp");
		}
		doConfirm(receipt, actorId);
	}

	@Transactional
	public void cancel(String receiptCode) {
		ImportReceipt receipt = receiptRepository.lockByReceiptCode(receiptCode)
				.orElseThrow(() -> new InventoryException("Không tìm thấy phiếu nhập"));
		if (!InventoryConstants.IMPORT_DRAFT.equals(receipt.getStatus())) {
			throw new InventoryException(
					"Chỉ hủy được phiếu nháp. Phiếu đã nhập kho thì dùng chức năng điều chỉnh tồn kho.");
		}
		receipt.setStatus(InventoryConstants.IMPORT_CANCELLED);
	}

	private void doConfirm(ImportReceipt receipt, Integer actorId) {
		for (ImportReceiptDetail d : detailRepository.findDetailedByReceiptId(receipt.getImportReceiptId())) {
			inventoryService.addStock(d.getVariant().getVariantId(), d.getQuantity(), actorId,
					InventoryConstants.TX_IMPORT, "Nhập kho theo phiếu " + receipt.getReceiptCode());
		}
		receipt.setStatus(InventoryConstants.IMPORT_CONFIRMED);
		receipt.setConfirmAt(LocalDateTime.now());
	}

	private String generateCode() {
		String code;
		do {
			code = "PN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
					+ ThreadLocalRandom.current().nextInt(10, 100);
		} while (receiptRepository.existsByReceiptCode(code));
		return code;
	}

	/** Hoàn tác phiếu đã nhập kho: trừ lại tồn kho và chuyển phiếu sang đã hủy */
	@Transactional
	public void reverse(String receiptCode, Integer actorId, String reason) {
		String r = reason == null ? "" : reason.trim();
		if (r.length() < 3) {
			throw new InventoryException("Vui lòng nhập lý do hoàn tác (ít nhất 3 ký tự)");
		}
		ImportReceipt receipt = receiptRepository.lockByReceiptCode(receiptCode)
				.orElseThrow(() -> new InventoryException("Không tìm thấy phiếu nhập"));
		if (!InventoryConstants.IMPORT_CONFIRMED.equals(receipt.getStatus())) {
			throw new InventoryException("Chỉ hoàn tác được phiếu đã nhập kho");
		}

		// Gộp các dòng cùng một biến thể
		Map<Integer, Integer> qtyByVariant = new LinkedHashMap<>();
		Map<Integer, String> labels = new LinkedHashMap<>();
		for (ImportReceiptDetail d : detailRepository.findDetailedByReceiptId(receipt.getImportReceiptId())) {
			Integer vid = d.getVariant().getVariantId();
			qtyByVariant.merge(vid, d.getQuantity(), Integer::sum);
			labels.putIfAbsent(vid,
					d.getVariant().getProduct().getProductName() + " (" + d.getVariant().getSku() + ")");
		}

		String shortReason = r.length() > 100 ? r.substring(0, 100) : r;
		for (Map.Entry<Integer, Integer> e : qtyByVariant.entrySet()) {
			inventoryService.removeStock(e.getKey(), e.getValue(), actorId, InventoryConstants.TX_ADJUST,
					"Hoàn tác phiếu nhập " + receipt.getReceiptCode() + ": " + shortReason, labels.get(e.getKey()));
		}

		receipt.setStatus(InventoryConstants.IMPORT_CANCELLED);
		String old = receipt.getNote() == null ? "" : receipt.getNote();
		String merged = (old.isEmpty() ? "" : old + " ") + "[Hoàn tác: " + shortReason + "]";
		receipt.setNote(merged.length() > 500 ? merged.substring(0, 500) : merged);
	}

	// ===================== Dữ liệu trả về cho giao diện =====================

	@Getter
	@AllArgsConstructor
	public static class ImportPage {
		private final Page<ImportReceipt> page;
		private final Map<Integer, User> creators;
	}

	@Getter
	@AllArgsConstructor
	public static class ImportView {
		private final ImportReceipt receipt;
		private final List<ImportReceiptDetail> details;
		private final User creator;
	}
}