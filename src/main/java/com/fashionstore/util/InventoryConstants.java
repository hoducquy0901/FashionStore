package com.fashionstore.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Các giá trị này phải nằm trong ràng buộc CHECK của DB */
public final class InventoryConstants {

    private InventoryConstants() {}

    // Trạng thái phiếu nhập
    public static final String IMPORT_DRAFT = "DRAFT";
    public static final String IMPORT_CONFIRMED = "CONFIRMED";
    public static final String IMPORT_CANCELLED = "CANCELLED";

    // Loại giao dịch kho
    public static final String TX_IMPORT = "IMPORT";
    public static final String TX_EXPORT = "SALE";          // xuất kho khi giao hàng
    public static final String TX_RETURN = "RETURN";
    public static final String TX_ADJUST = "ADJUSTMENT";    // kiểm kê / điều chỉnh / hoàn tác phiếu nhập
    
    /** Đánh dấu hướng thay đổi trong ghi chú của giao dịch ADJUST (cột quantity chỉ nhận số dương) */
    public static final String NOTE_UP = "[+] ";
    public static final String NOTE_DOWN = "[-] ";

    public static final String DEFAULT_LOCATION = "MAIN";

    /** Số lượng có thể bán nhỏ hơn hoặc bằng mức này thì cảnh báo sắp hết */
    public static final int LOW_STOCK_THRESHOLD = 5;

    public static final Map<String, String> IMPORT_STATUS;
    public static final Map<String, String> TX_LABELS;

    static {
        Map<String, String> s = new LinkedHashMap<>();
        s.put(IMPORT_DRAFT, "Nháp");
        s.put(IMPORT_CONFIRMED, "Đã nhập kho");
        s.put(IMPORT_CANCELLED, "Đã hủy");
        IMPORT_STATUS = Collections.unmodifiableMap(s);

        Map<String, String> t = new LinkedHashMap<>();
        t.put(TX_IMPORT, "Nhập kho");
        t.put(TX_EXPORT, "Xuất kho (giao hàng)");
        t.put(TX_RETURN, "Hàng trả về kho");
        t.put(TX_ADJUST, "Kiểm kê / điều chỉnh");
        TX_LABELS = Collections.unmodifiableMap(t);
    }
}