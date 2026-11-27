# FashionStore

Website bán hàng thời trang xây dựng bằng Spring Boot 4, Java 17, Thymeleaf và SQL Server. README này hướng dẫn dựng database và chạy ứng dụng trên Windows hoặc bằng Docker Compose.

## Yêu cầu

- Git (nếu clone từ repository).
- Java JDK 17.
- SQL Server 2022 và SQL Server Management Studio (SSMS), hoặc Docker Desktop.
- Có thể dùng Maven Wrapper đi kèm (`mvnw.cmd`), không cần cài Maven riêng.

## Cách 1: Chạy bằng Docker Compose

Đây là cách dựng nhanh nhất; Compose khởi động SQL Server, tạo database từ `scriptSQL.sql` rồi chạy ứng dụng.

1. Tạo file `.env` ở thư mục gốc (cùng cấp `compose.yaml`) với mật khẩu SQL Server:

   ```env
   DB_PASSWORD=Strong_Password_123!
   ```

   Dùng mật khẩu đủ mạnh theo yêu cầu SQL Server. Không commit `.env` lên Git.

2. Mở PowerShell tại thư mục dự án và chạy:

   ```powershell
   docker compose up --build
   ```

   Lần đầu sẽ tải image và có thể mất vài phút. Đợi dịch vụ `app` khởi động xong.

3. Mở [http://localhost:8080](http://localhost:8080). Dừng các dịch vụ bằng `Ctrl+C`; để chạy nền, dùng `docker compose up --build -d`. Dừng nền bằng `docker compose down`.

Compose giữ dữ liệu database và ảnh tải lên trong Docker volumes. Muốn xóa cả dữ liệu đã lưu, dùng `docker compose down -v`.

## Cách 2: Chạy ứng dụng trực tiếp

### 1. Tạo database

Khởi động SQL Server, bật TCP/IP cổng `1433`, sau đó mở `scriptSQL.sql` bằng SSMS và thực thi toàn bộ script. Script sẽ tạo database `FashionStoreDB`, các bảng và dữ liệu mẫu.

Tài khoản kết nối mặc định của ứng dụng là `sa`. Nếu SQL Server dùng tài khoản khác, cập nhật username trong `src/main/resources/application.properties`.

### 2. Cấu hình kết nối

Đặt biến môi trường `DB_PASSWORD` bằng mật khẩu SQL Server trước khi chạy ứng dụng. Ví dụ trong PowerShell:

```powershell
$env:DB_PASSWORD = "Strong_Password_123!"
```

Ứng dụng đọc cấu hình kết nối từ `src/main/resources/application.properties`. Hibernate được cấu hình `ddl-auto=none`, do đó cần tạo database trước khi khởi động.

### 3. Khởi động

Tại thư mục gốc dự án, chạy:

```powershell
.\mvnw.cmd spring-boot:run
```

Hoặc trong Eclipse: import project dưới dạng Maven project, cấu hình biến môi trường `DB_PASSWORD` cho Run Configuration, rồi chạy `com.fashionstore.FashionStoreApplication`.

Mở [http://localhost:8080](http://localhost:8080). Dừng ứng dụng bằng `Ctrl+C` trong terminal hoặc nút Stop trong Eclipse.

## Tài khoản và quyền quản trị

Database mẫu có dữ liệu tài khoản trong `scriptSQL.sql`. Để dùng tài khoản tự tạo làm admin, đăng ký tại `/register`, rồi cập nhật quyền trong SSMS:

```sql
UPDATE [USER]
SET roleId = (SELECT roleId FROM ROLE WHERE roleName = 'ADMIN')
WHERE email = 'email-cua-ban@example.com';
```

Đăng xuất rồi đăng nhập lại để quyền mới có hiệu lực. Khu vực quản trị ở `/admin`.

## Cấu hình VNPay (không bắt buộc)

Thanh toán VNPay cần thông tin sandbox. Thiết lập các biến môi trường sau trước khi chạy:

```powershell
$env:VNPAY_TMN_CODE = "ma-website"
$env:VNPAY_HASH_SECRET = "chuoi-bi-mat"
```

URL sandbox và URL trả về được cấu hình trong `application.properties`. Khi chạy ở localhost, người dùng có thể quay về trang return; IPN từ VNPay cần website được truy cập từ Internet.

## Tải ảnh sản phẩm

Ảnh admin tải lên được lưu trong thư mục `uploads` mặc định. Docker Compose lưu ảnh trong volume riêng để ảnh vẫn còn sau khi container được tạo lại. Để thay đường dẫn khi chạy trực tiếp, đặt biến môi trường `PRODUCT_IMAGE_DIR`.

## Một số lỗi thường gặp

- **Không kết nối được SQL Server:** kiểm tra dịch vụ SQL Server đang chạy, TCP/IP đã bật, cổng `1433` mở và `DB_PASSWORD` đúng.
- **Không tìm thấy database hoặc bảng:** chạy `scriptSQL.sql` trên SQL Server trước khi chạy ứng dụng.
- **Cổng 8080 hoặc 1433 đã được dùng:** dừng dịch vụ đang chiếm cổng hoặc đổi ánh xạ cổng trong `compose.yaml` (Docker) và `server.port` / datasource URL trong `application.properties` (chạy trực tiếp).
- **Lỗi Java version:** kiểm tra `java -version` hiển thị Java 17 và Eclipse đang dùng JDK 17.

## Công nghệ

Java 17 · Spring Boot 4.1.1 · Spring MVC · Spring Security · Spring Data JPA · Thymeleaf · Microsoft SQL Server · Maven
