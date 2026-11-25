package com.fashionstore.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductImageStorageService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 12_000_000L;
    private final Path imageDirectory;

    public ProductImageStorageService(@Value("${app.product-image-dir:uploads}") String directory) {
        this.imageDirectory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh sản phẩm");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 5 MB");
        }

        String extension = detectExtension(file);
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(imageDirectory);
            Path destination = imageDirectory.resolve(filename).normalize();
            if (!destination.getParent().equals(imageDirectory)) {
                throw new IllegalArgumentException("Tên ảnh không hợp lệ");
            }
            try (var input = file.getInputStream()) {
                Files.copy(input, destination);
            }
            return "/uploads/" + filename;
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh sản phẩm", exception);
        }
    }

    public void deleteIfManaged(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return;
        }
        String filename = imageUrl.substring("/uploads/".length());
        Path target = imageDirectory.resolve(filename).normalize();
        if (!target.getParent().equals(imageDirectory)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể xóa file ảnh", exception);
        }
    }

    private String detectExtension(MultipartFile file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                throw new IllegalArgumentException("Tệp tải lên không phải ảnh hợp lệ");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPEG, PNG hoặc GIF");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_PIXELS || reader.getWidth(0) > 8000 || reader.getHeight(0) > 8000) {
                    throw new IllegalArgumentException("Kích thước ảnh vượt quá giới hạn cho phép");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("Tệp tải lên không phải ảnh hợp lệ");
                }
                return switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg", "jpg" -> "jpg";
                    case "png" -> "png";
                    case "gif" -> "gif";
                    default -> throw new IllegalArgumentException("Chỉ hỗ trợ ảnh JPEG, PNG hoặc GIF");
                };
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc ảnh tải lên", exception);
        }
    }
}
