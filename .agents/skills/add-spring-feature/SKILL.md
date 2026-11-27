---
name: add-spring-feature
description: Thêm luồng nghiệp vụ FashionStore theo kiến trúc Spring MVC, service, repository và Thymeleaf hiện có.
---

# Thêm tính năng Spring

1. Đọc AGENTS.md; xác định hành vi, quyền, URL và nghiệp vụ liên quan.
2. Tìm chức năng gần nhất làm mẫu; đọc controller, service, repository, entity/DTO và template.
3. Đối chiếu schema trong entity và scriptSQL.sql. Không tự đặt field, bảng, trạng thái, URL hoặc quy tắc. Hỏi nếu thiếu thông tin.
4. Giữ controller mỏng; đặt nghiệp vụ và transaction trong service; truy vấn dữ liệu trong repository.
5. Dùng DTO/form object và Jakarta Bean Validation. Hiển thị lỗi an toàn và giữ lại dữ liệu form hợp lệ.
6. Kiểm tra quyền phía server. Giỏ hàng, địa chỉ và đơn hàng phải thuộc tài khoản đăng nhập; /admin/** chỉ dành cho ADMIN. Không tin user id, giá hoặc tổng tiền từ form.
7. Giữ CSRF và quy ước giao diện hiện có. Thêm test cho nhánh nghiệp vụ và quyền liên quan.
8. Chạy mvnw.cmd test khi được yêu cầu xác minh; báo rõ nếu cần SQL Server hoặc môi trường đặc biệt.
9. Không đọc .env, không dùng VNPay thật, không xóa file/dữ liệu và không thêm dependency nếu chưa được yêu cầu hoặc giải thích.

Comment mới trong code viết bằng tiếng Việt.