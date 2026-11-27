---
name: update-order-inventory-flow
description: Thay đổi nghiệp vụ đơn hàng, thanh toán hoặc tồn kho mà vẫn giữ transaction và quy tắc FashionStore.
---

# Sửa luồng đơn hàng và tồn kho

1. Đọc nghiệp vụ trong AGENTS.md; đối chiếu OrderService, OrderManagementService, InventoryService, ImportService, entity, repository và test.
2. Không thêm hoặc đổi trạng thái nếu chưa được yêu cầu và xác định rõ. Giữ transition hiện có.
3. Đặt các bước cùng nghiệp vụ trong transaction. Dùng cơ chế khóa/cập nhật có điều kiện hiện có để tránh bán vượt tồn và lỗi đồng thời.
4. Giữ các bất biến: đặt hàng tăng reservedQuantity nhưng không giảm quantity; hủy giải phóng phần giữ đúng một lần; chuyển sang SHIPPING giảm cả quantity và reservedQuantity; RETURNED cộng lại quantity; chỉ xác nhận phiếu nhập mới cộng tồn; kiểm kê không thấp hơn lượng đã giữ.
5. Kiểm tra trạng thái nguồn, quyền ADMIN và idempotency trước khi ghi lịch sử hoặc biến động kho. Không tin số lượng, giá hoặc trạng thái từ browser.
6. VNPay chỉ được cập nhật khi chữ ký và số tiền hợp lệ; return/IPN lặp phải an toàn. Không gọi giao dịch thật trong test.
7. Viết test cho transition đúng/sai, số lượng biên, rollback và request lặp. Chỉ dùng SQL Server test biệt lập.
8. Báo quy tắc chưa rõ để xác nhận; không tự sửa schema, dữ liệu hoặc lịch sử đơn hàng.

Không đọc .env hoặc ghi bí mật vào log/test.