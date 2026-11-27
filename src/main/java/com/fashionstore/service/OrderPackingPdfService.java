package com.fashionstore.service;

import com.fashionstore.entity.OrderDetail;
import com.fashionstore.entity.Orders;
import com.fashionstore.exception.OrderException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderPackingPdfService {
    private static final float PAPER_WIDTH = 80f * 72f / 25.4f;
    private static final float QR_SIZE = 92f;
    private static final float TEXT_LEADING = 12f;
    private final Path outputDirectory;

    public OrderPackingPdfService(
            @Value("${app.order-pdf-dir:src/main/resources/static/images/orders}") String outputDirectory) {
        this.outputDirectory = Path.of(outputDirectory).toAbsolutePath().normalize();
    }

    public Path create(Orders order, List<OrderDetail> details) {
        try {
            Files.createDirectories(outputDirectory);
            Path path = pdfPath(order.getOrderCode());
            Files.write(path, render(order, details));
            return path;
        } catch (IOException e) {
            throw new OrderException("Không thể tạo file PDF đóng hàng");
        }
    }

    public Path find(String orderCode) {
        Path path = pdfPath(orderCode);
        if (!Files.isRegularFile(path)) {
            throw new OrderException("Chưa có phiếu PDF cho đơn hàng này");
        }
        return path;
    }

    private Path pdfPath(String orderCode) {
        String safeCode = orderCode == null ? "" : orderCode.replaceAll("[^A-Za-z0-9_-]", "");
        if (safeCode.isBlank()) {
            throw new OrderException("Mã đơn hàng không hợp lệ");
        }
        return outputDirectory.resolve(safeCode + ".pdf").normalize();
    }

    private byte[] render(Orders order, List<OrderDetail> details) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("FASHIONSTORE - PHIEU DONG GOI");
        lines.add("Ma don: " + order.getOrderCode());
        lines.add("Ngay dat: " + order.getCreatedAtText());
        lines.add("--------------------------------");
        for (OrderDetail detail : details) {
            var variant = detail.getVariant();
            String productName = variant.getProduct().getProductName();
            String options = (variant.getColor() == null ? "" : variant.getColor().getColorName()) + " / "
                    + (variant.getSize() == null ? "" : variant.getSize().getSizeName());
            addWrapped(lines, productName + " | SKU " + variant.getSku());
            addWrapped(lines, options + " | SL: " + detail.getQuantity());
        }
        lines.add("--------------------------------");
        addWrapped(lines, "Tong thanh toan: " + order.getFinalAmount() + " VND");
        return createPdf(lines, order.getOrderCode());
    }

    private byte[] createPdf(List<String> lines, String orderCode) throws IOException {
        float pageHeight = 18f + lines.size() * TEXT_LEADING + 8f + QR_SIZE + 6f + 12f + 14f;
        StringBuilder content = new StringBuilder();
        float textY = pageHeight - 27f;
        for (int index = 0; index < lines.size(); index++) {
            boolean heading = index == 0;
            content.append("BT /").append(heading ? "F2 10" : "F1 8")
                    .append(" Tf 12 ").append(textY).append(" Td (")
                    .append(pdfText(lines.get(index))).append(") Tj ET\n");
            textY -= TEXT_LEADING;
        }

        BitMatrix qr = createQr(orderCode);
        float qrLeft = (PAPER_WIDTH - QR_SIZE) / 2f;
        float qrBottom = textY - 4f - QR_SIZE;
        appendQr(content, qr, qrLeft, qrBottom, QR_SIZE);
        content.append("BT /F1 8 Tf 12 ").append(qrBottom - 12f)
                .append(" Td (QR ma don: ").append(pdfText(orderCode)).append(") Tj ET\n");

        byte[] stream = content.toString().getBytes(StandardCharsets.US_ASCII);
        List<byte[]> objects = List.of(
                ascii("<< /Type /Catalog /Pages 2 0 R >>"),
                ascii("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"),
                ascii("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAPER_WIDTH + " " + pageHeight
                        + "] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>"),
                ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"),
                ascii("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"),
                concat(ascii("<< /Length " + stream.length + " >>\nstream\n"), stream, ascii("\nendstream")));
        return assemblePdf(objects);
    }

    private BitMatrix createQr(String orderCode) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            return new QRCodeWriter().encode(orderCode, BarcodeFormat.QR_CODE, 1, 1, hints);
        } catch (WriterException e) {
            throw new OrderException("Không thể tạo mã QR cho đơn hàng");
        }
    }

    private void appendQr(StringBuilder content, BitMatrix matrix, float left, float bottom, float size) {
        float moduleSize = size / matrix.getWidth();
        content.append("q 0 g\n");
        for (int row = 0; row < matrix.getHeight(); row++) {
            float y = bottom + (matrix.getHeight() - row - 1) * moduleSize;
            for (int column = 0; column < matrix.getWidth(); column++) {
                if (matrix.get(column, row)) {
                    float x = left + column * moduleSize;
                    content.append(x).append(' ').append(y).append(' ').append(moduleSize).append(' ')
                            .append(moduleSize).append(" re\n");
                }
            }
        }
        content.append("f Q\n");
    }

    private void addWrapped(List<String> lines, String value) {
        String text = asciiText(value);
        int maxLength = 38;
        while (text.length() > maxLength) {
            int breakAt = text.lastIndexOf(' ', maxLength);
            if (breakAt < 1) {
                breakAt = maxLength;
            }
            lines.add(text.substring(0, breakAt));
            text = text.substring(breakAt).stripLeading();
        }
        lines.add(text);
    }

    private String pdfText(String value) {
        return asciiText(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String asciiText(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^\\x20-\\x7E]", "?");
    }

    private byte[] assemblePdf(List<byte[]> objects) throws IOException {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(ascii("%PDF-1.4\n%FashionStore\n"));
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(pdf.size());
            pdf.write(ascii((index + 1) + " 0 obj\n"));
            pdf.write(objects.get(index));
            pdf.write(ascii("\nendobj\n"));
        }
        int xrefOffset = pdf.size();
        pdf.write(ascii("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (Integer offset : offsets) {
            pdf.write(ascii(String.format("%010d 00000 n \n", offset)));
        }
        pdf.write(ascii("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                + xrefOffset + "\n%%EOF"));
        return pdf.toByteArray();
    }

    private byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] concat(byte[] first, byte[] second, byte[] third) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(first);
        output.write(second);
        output.write(third);
        return output.toByteArray();
    }
}
