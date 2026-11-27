---
name: add-thymeleaf-screen
description: Tạo hoặc mở rộng trang Thymeleaf cho khách hàng hoặc admin theo cấu trúc FashionStore.
---

# Làm màn hình Thymeleaf

1. Đọc AGENTS.md và xem controller/template gần nhất, header, admin navigation và CSS hiện có.
2. Xác nhận URL, quyền và model từ mã nguồn. Không tự đặt route, field hoặc nghiệp vụ.
3. Controller chuẩn bị model và điều hướng; không đưa truy vấn hoặc nghiệp vụ vào template.
4. Dùng form object/DTO với Bean Validation, th:object, th:field và BindingResult theo mẫu hiện có. Hiển thị lỗi cạnh trường và giữ dữ liệu người dùng nhập.
5. Giữ CSRF trên mọi form thay đổi dữ liệu. Không đổi POST thành GET để né bảo vệ. Giữ phân quyền server cho /admin/**.
6. Escape nội dung hiển thị; tránh th:utext với dữ liệu người dùng. Dùng image paths và fragments theo cách project hiện có.
7. Có trạng thái danh sách rỗng, thông báo thành công/lỗi và điều hướng phù hợp.
8. Kiểm tra phân quyền và hành vi form; chạy mvnw.cmd test nếu được yêu cầu. Không thêm dependency UI nếu chưa giải thích.

Không đọc .env hoặc đưa bí mật vào template, log hay test.