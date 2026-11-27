---
name: verify-store-feature
description: Lập và chạy kiểm tra FashionStore bằng Maven Wrapper, không dùng dữ liệu hoặc dịch vụ thanh toán thật.
---

# Xác minh tính năng

1. Đọc AGENTS.md, pom.xml và test hiện có; xác định yêu cầu database, Spring Security và cấu hình ngoài.
2. Chọn unit test cho service; MockMvc cho form, validation, CSRF và role; integration test chỉ với SQL Server biệt lập.
3. Bao gồm đường đi thành công, dữ liệu sai, quyền sai, transition sai và request lặp ở các luồng có side effect.
4. Chạy mvnw.cmd test khi người dùng yêu cầu hoặc task cần xác minh. Nếu cần kiểm tra đóng gói, dùng mvnw.cmd -DskipTests package.
5. Không gọi VNPay thật, không gửi email/thanh toán thật, không dùng database production, không đọc .env và không xóa dữ liệu.
6. Nếu chưa có automation phù hợp, đưa checklist thao tác với role và dữ liệu giả lập; nêu rõ điều kiện còn thiếu.
7. Báo lệnh, kết quả, test thất bại và phần không thể kiểm tra. Không gọi build thành công là browser test.
8. Không thêm dependency hoặc fixture có thể phá dữ liệu nếu chưa được xác nhận.