package com.fashionstore.service;

import com.fashionstore.dto.AdminImageRow;
import com.fashionstore.dto.AdminProductEditor;
import com.fashionstore.dto.AdminProductForm;
import com.fashionstore.dto.AdminProductRow;
import com.fashionstore.dto.AdminSelectOption;
import com.fashionstore.dto.AdminVariantForm;
import com.fashionstore.dto.AdminVariantRow;
import com.fashionstore.entity.Category;
import com.fashionstore.entity.Color;
import com.fashionstore.entity.Product;
import com.fashionstore.entity.ProductImage;
import com.fashionstore.entity.ProductVariant;
import com.fashionstore.entity.Size;
import com.fashionstore.repository.CategoryRepository;
import com.fashionstore.repository.ColorRepository;
import com.fashionstore.repository.ProductImageRepository;
import com.fashionstore.repository.ProductRepository;
import com.fashionstore.repository.ProductVariantRepository;
import com.fashionstore.repository.SizeRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final ProductImageStorageService imageStorage;
    private final InventoryService inventoryService;

    public AdminProductService(ProductRepository productRepository,
                               ProductVariantRepository variantRepository,
                               ProductImageRepository imageRepository,
                               CategoryRepository categoryRepository,
                               SizeRepository sizeRepository,
                               ColorRepository colorRepository,
                               ProductImageStorageService imageStorage,
                               InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.categoryRepository = categoryRepository;
        this.sizeRepository = sizeRepository;
        this.colorRepository = colorRepository;
        this.imageStorage = imageStorage;
        this.inventoryService = inventoryService;
    }

    @Transactional(readOnly = true)
    public Page<AdminProductRow> search(String status, String keyword, int page, int pageSize) {
        String safeStatus = status == null ? "" : status.trim();
        String safeKeyword = keyword == null ? "" : keyword.trim();
        Page<Product> products = productRepository.searchAdmin(safeStatus, safeKeyword,
                PageRequest.of(Math.max(0, page), pageSize));
        List<Integer> productIds = products.getContent().stream().map(Product::getProductId).toList();
        if (productIds.isEmpty()) {
            return new PageImpl<>(List.of(), products.getPageable(), products.getTotalElements());
        }

        Map<Integer, String> categories = categoryRepository.findAllById(
                        products.getContent().stream().map(Product::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(Category::getCategoryId, Category::getCategoryName));
        Map<Integer, String> mainImages = new HashMap<>();
        for (ProductImage image : imageRepository.findForProductIds(productIds)) {
            mainImages.putIfAbsent(image.getProduct().getProductId(), image.getImageUrl());
        }
        Map<Integer, Long> variantCounts = new HashMap<>();
        for (Object[] count : variantRepository.countByProductIds(productIds)) {
            variantCounts.put((Integer) count[0], (Long) count[1]);
        }
        List<AdminProductRow> rows = products.getContent().stream()
                .map(product -> new AdminProductRow(product.getProductId(), product.getProductName(),
                        product.getBrand(), product.getBasePrice(), product.getStatus(),
                        categories.getOrDefault(product.getCategoryId(), "-"),
                        mainImages.get(product.getProductId()), variantCounts.getOrDefault(product.getProductId(), 0L)))
                .toList();
        return new PageImpl<>(rows, products.getPageable(), products.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminProductEditor getEditor(Integer productId) {
        Product product = getProduct(productId);
        List<ProductVariant> variants = variantRepository.findAllForAdmin(productId);
        Map<Integer, String> variantLabels = variants.stream().collect(Collectors.toMap(
                ProductVariant::getVariantId, this::variantLabel));
        List<AdminVariantRow> variantRows = variants.stream().map(variant -> {
            AdminVariantRow row = new AdminVariantRow();
            row.setVariantId(variant.getVariantId());
            row.setSizeName(variant.getSize().getSizeName());
            row.setColorName(variant.getColor().getColorName());
            row.setColorHex(variant.getColor().getColorHex());
            row.setSizeId(variant.getSize().getSizeId());
            row.setColorId(variant.getColor().getColorId());
            row.setSku(variant.getSku());
            row.setPrice(variant.getPrice());
            row.setStatus(variant.getStatus());
            return row;
        }).toList();
        List<AdminImageRow> imageRows = imageRepository.findByProduct_ProductIdOrderByMainDescImageIdAsc(productId)
                .stream().map(image -> new AdminImageRow(image.getImageId(), image.getImageUrl(),
                        Boolean.TRUE.equals(image.getMain()), image.getVariantId() == null
                        ? "Toàn sản phẩm" : variantLabels.getOrDefault(image.getVariantId(),
                        "Biến thể #" + image.getVariantId())))
                .toList();
        return new AdminProductEditor(toForm(product), variantRows, imageRows);
    }

    @Transactional
    public Integer saveProduct(AdminProductForm form) {
        if (!categoryRepository.existsById(form.getCategoryId())) {
            throw new IllegalArgumentException("Danh mục không tồn tại");
        }
        Product product = form.getProductId() == null ? new Product() : getProduct(form.getProductId());
        boolean creating = product.getProductId() == null;
        product.setCategoryId(form.getCategoryId());
        product.setProductName(form.getProductName().trim());
        product.setDescription(blankToNull(form.getDescription()));
        product.setBrand(blankToNull(form.getBrand()));
        product.setBasePrice(form.getBasePrice());
        product.setStatus(form.getStatus());
        if (creating) {
            product.setCreatedAt(LocalDateTime.now());
        } else {
            product.setUpdatedAt(LocalDateTime.now());
        }
        return productRepository.save(product).getProductId();
    }

    @Transactional
    public Integer saveVariant(Integer productId, Integer variantId, AdminVariantForm form) {
        Product product = getProduct(productId);
        Size size = sizeRepository.findById(form.getSizeId())
                .orElseThrow(() -> new IllegalArgumentException("Kích cỡ không tồn tại"));
        Color color = colorRepository.findById(form.getColorId())
                .orElseThrow(() -> new IllegalArgumentException("Màu sắc không tồn tại"));
        String sku = form.getSku().trim();

        ProductVariant variant = variantId == null ? new ProductVariant() : getVariantForProduct(productId, variantId);
        boolean skuUsed = variantId == null
                ? variantRepository.existsBySkuIgnoreCase(sku)
                : variantRepository.existsBySkuIgnoreCaseAndVariantIdNot(sku, variantId);
        if (skuUsed) {
            throw new IllegalArgumentException("SKU đã được sử dụng");
        }
        boolean combinationUsed = variantId == null
                ? variantRepository.existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorId(
                        productId, form.getSizeId(), form.getColorId())
                : variantRepository.existsByProduct_ProductIdAndSize_SizeIdAndColor_ColorIdAndVariantIdNot(
                        productId, form.getSizeId(), form.getColorId(), variantId);
        if (combinationUsed) {
            throw new IllegalArgumentException("Sản phẩm đã có biến thể với kích cỡ và màu này");
        }

        variant.setProduct(product);
        variant.setSize(size);
        variant.setColor(color);
        variant.setSku(sku);
        variant.setPrice(form.getPrice());
        variant.setStatus(form.getStatus());
        ProductVariant savedVariant = variantRepository.save(variant);
        inventoryService.ensureDefaultInventory(savedVariant.getVariantId());
        return savedVariant.getVariantId();
    }

    @Transactional
    public void deactivateVariant(Integer productId, Integer variantId) {
        ProductVariant variant = getVariantForProduct(productId, variantId);
        variant.setStatus("INACTIVE");
        variantRepository.save(variant);
    }

    @Transactional
    public void addImage(Integer productId, Integer variantId, MultipartFile file, boolean setMain) {
        Product product = getProduct(productId);
        if (variantId != null) {
            getVariantForProduct(productId, variantId);
        }
        String imageUrl = imageStorage.store(file);
        try {
            List<ProductImage> images = imageRepository.findByProduct_ProductIdOrderByMainDescImageIdAsc(productId);
            boolean makeMain = setMain || images.isEmpty();
            if (makeMain) {
                images.forEach(image -> image.setMain(false));
                imageRepository.saveAll(images);
            }
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(imageUrl);
            image.setVariantId(variantId);
            image.setMain(makeMain);
            imageRepository.save(image);
        } catch (RuntimeException exception) {
            imageStorage.deleteIfManaged(imageUrl);
            throw exception;
        }
    }

    @Transactional
    public void setMainImage(Integer productId, Integer imageId) {
        getProduct(productId);
        ProductImage selected = getImageForProduct(productId, imageId);
        List<ProductImage> images = imageRepository.findByProduct_ProductIdOrderByMainDescImageIdAsc(productId);
        images.forEach(image -> image.setMain(image.getImageId().equals(selected.getImageId())));
        imageRepository.saveAll(images);
    }

    @Transactional
    public void deleteImage(Integer productId, Integer imageId) {
        ProductImage selected = getImageForProduct(productId, imageId);
        boolean wasMain = Boolean.TRUE.equals(selected.getMain());
        String imageUrl = selected.getImageUrl();
        imageRepository.delete(selected);
        List<ProductImage> remaining = new ArrayList<>(
                imageRepository.findByProduct_ProductIdOrderByMainDescImageIdAsc(productId));
        if (wasMain && !remaining.isEmpty()) {
            remaining.forEach(image -> image.setMain(false));
            remaining.get(0).setMain(true);
            imageRepository.saveAll(remaining);
        }
        imageStorage.deleteIfManaged(imageUrl);
    }

    @Transactional(readOnly = true)
    public List<AdminSelectOption> getCategoryOptions() {
        return categoryRepository.findAllByOrderByCategoryNameAsc().stream()
                .map(category -> new AdminSelectOption(category.getCategoryId(), category.getCategoryName(), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminSelectOption> getSizeOptions() {
        return sizeRepository.findAllByOrderBySizeNameAsc().stream()
                .map(size -> new AdminSelectOption(size.getSizeId(), size.getSizeName(), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminSelectOption> getColorOptions() {
        return colorRepository.findAllByOrderByColorNameAsc().stream()
                .map(color -> new AdminSelectOption(color.getColorId(), color.getColorName(), color.getColorHex()))
                .toList();
    }

    private Product getProduct(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    }

    private ProductVariant getVariantForProduct(Integer productId, Integer variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể"));
        if (!productId.equals(variant.getProduct().getProductId())) {
            throw new IllegalArgumentException("Biến thể không thuộc sản phẩm này");
        }
        return variant;
    }

    private ProductImage getImageForProduct(Integer productId, Integer imageId) {
        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh sản phẩm"));
        if (image.getProduct() == null || !productId.equals(image.getProduct().getProductId())) {
            throw new IllegalArgumentException("Ảnh không thuộc sản phẩm này");
        }
        return image;
    }

    private AdminProductForm toForm(Product product) {
        AdminProductForm form = new AdminProductForm();
        form.setProductId(product.getProductId());
        form.setCategoryId(product.getCategoryId());
        form.setProductName(product.getProductName());
        form.setDescription(product.getDescription());
        form.setBrand(product.getBrand());
        form.setBasePrice(product.getBasePrice());
        form.setStatus(product.getStatus());
        return form;
    }

    private String variantLabel(ProductVariant variant) {
        return variant.getColor().getColorName() + " / " + variant.getSize().getSizeName() + " (" + variant.getSku() + ")";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
