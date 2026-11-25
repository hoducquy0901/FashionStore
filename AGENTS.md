# FashionStore

Website bán thời trang xây dựng bằng Spring Boot: khách hàng xem sản phẩm, thêm giỏ hàng, đặt hàng và thanh toán (COD, chuyển khoản, VNPay); quản trị viên quản lý sản phẩm, danh mục, đơn hàng, nhập hàng và tồn kho.

> Tài liệu này được soạn từ phần code đã phát triển cùng nhau. Một số chi tiết phụ thuộc vào database thực tế của bạn (tên cột, giá trị ràng buộc CHECK). Những chỗ đó được đánh dấu **(đối chiếu)**, hãy kiểm tra lại với project của bạn trước khi nộp hoặc công bố.

## Mục lục

1. [Công nghệ](#1-công-nghệ)
2. [Chức năng](#2-chức-năng)
3. [Kiến trúc và cấu trúc project](#3-kiến-trúc-và-cấu-trúc-project)
4. [Cài đặt và chạy](#4-cài-đặt-và-chạy)
5. [Cấu hình](#5-cấu-hình)
6. [Danh sách đường dẫn (URL)](#6-danh-sách-đường-dẫn-url)
7. [Cơ sở dữ liệu](#7-cơ-sở-dữ-liệu)
8. [Quy tắc nghiệp vụ chính](#8-quy-tắc-nghiệp-vụ-chính)
9. [Bảo mật](#9-bảo-mật)
10. [Kiểm thử và tài liệu đi kèm](#10-kiểm-thử-và-tài-liệu-đi-kèm)
11. [Hạn chế và hướng phát triển](#11-hạn-chế-và-hướng-phát-triển)
12. [Lưu ý khi dùng Git](#12-lưu-ý-khi-dùng-git)

## 1. Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ, build | Java 17, Maven |
| Framework | Spring Boot 4.1.1 (Spring MVC, Spring Security 7, Spring Data JPA, Hibernate 7) |
| Giao diện | Thymeleaf, HTML, CSS, JavaScript thuần |
| Cơ sở dữ liệu | Microsoft SQL Server (database `FashionStoreDB`) |
| Hỗ trợ | Lombok, Bean Validation |
| Thanh toán online | VNPay (môi trường sandbox) |
| IDE | Eclipse (Spring Boot Devtools) |

## 2. Chức năng

### Khách hàng

- **Trang chủ, danh mục, chi tiết sản phẩm:** xem sản phẩm mới, lọc theo danh mục, chọn màu và size, xem giá theo từng biến thể, xem nhiều ảnh.
- **Đăng ký, đăng nhập, đăng xuất:** mật khẩu mã hóa BCrypt, phân quyền `CUSTOMER` và `ADMIN`.
- **Giỏ hàng:** thêm (từ trang chủ hoặc trang chi tiết), đổi số lượng, xóa, "Mua ngay", icon giỏ hàng hiển thị số lượng.
- **Đặt hàng:** chọn hoặc nhập địa chỉ giao hàng (có nút điền thông tin tài khoản), chọn phương thức thanh toán, ghi chú. Hệ thống kiểm tra và giữ chỗ tồn kho.
- **Thanh toán:** COD, chuyển khoản ngân hàng (hiển thị thông tin chuyển khoản, admin xác nhận), thanh toán online qua VNPay (có thể thanh toán lại).
- **Đơn hàng của tôi:** xem danh sách, chi tiết, hủy đơn khi còn chờ xác nhận.

### Quản trị (`/admin/**`, chỉ vai trò ADMIN)

- **Quản lý đơn hàng:** lọc, tìm kiếm, đổi trạng thái theo quy trình, xác nhận đã thanh toán, ghi nhận hoàn tiền, xem lịch sử đơn.
- **Quản lý danh mục:** 2 cấp (gốc và con), ẩn hoặc hiện, xóa khi chưa có dữ liệu liên quan.
- **Quản lý sản phẩm:** thêm, sửa, ngừng bán; quản lý biến thể (màu, size, giá, SKU) và hình ảnh (upload nhiều ảnh, chọn ảnh chính, xóa); thêm màu, size mới.
- **Nhập hàng:** tạo phiếu nhập (nháp hoặc nhập kho ngay), xác nhận, hủy phiếu nháp, hoàn tác phiếu đã nhập.
- **Tồn kho:** xem tồn thực, đang giữ chỗ, có thể bán; cảnh báo sắp hết và hết hàng; kiểm kê điều chỉnh; lịch sử xuất nhập kho.
- **Tự động:** hủy đơn thanh toán VNPay quá 30 phút chưa trả tiền và trả hàng giữ chỗ về kho (chạy mỗi 5 phút).

## 3. Kiến trúc và cấu trúc project

```
Trình duyệt (Thymeleaf + CSS + JS)
      │  HTTP
      ▼
Spring Security  (đăng nhập, phân quyền, CSRF)
      ▼
Controller       nhận request, kiểm tra dữ liệu nhập, chọn view
      ▼
Service          nghiệp vụ, @Transactional
      ▼
Repository       Spring Data JPA / Hibernate
      ▼
SQL Server       bảng, khóa ngoại, ràng buộc CHECK
```

```
src/main/java/com/fashionstore
├── FashionStoreApplication.java
├── config        SecurityConfig, WebConfig, SchedulingConfig, VnpayProperties
├── controller    Home, Category, Product, Login, Auth, Cart, Checkout, Order, Vnpay,
│                 GlobalModelAdvice, AdminOrder, AdminImport, AdminInventory,
│                 AdminCategory, AdminProduct
├── DTO           RegisterRequest, CheckoutForm, CategoryForm, ProductForm
├── entity        User, Role, Category, Product, ProductImage, ProductVariant, Color, Size,
│                 Cart, CartItem, Address, Orders, OrderDetail, OrderStatusHistory, Payment,
│                 Inventory, InventoryTransaction, ImportReceipt, ImportReceiptDetail
├── exception     EmailAlreadyExists, Order, Inventory, Catalog exceptions
├── repository    Spring Data JPA repository cho từng entity
├── service       AuthService, CartService, OrderService, OrderManagementService,
│                 PaymentService, VnpayService, ImportService, InventoryService,
│                 ProductAdminService, CategoryAdminService, FileStorageService,
│                 FormTokenService, CurrentUserService, OrderExpiryJob
└── util          OrderLabels, InventoryConstants, CatalogConstants

src/main/resources
├── templates     home, login, register, product-detail, cart, checkout, orders,
│                 order-detail, payment-result, fragments/, admin/
├── static        css/style.css, js/*.js, images/products/ (ảnh mẫu)
└── application.properties

uploads/products  ảnh sản phẩm do admin tải lên (ngoài resources, không đưa lên Git)
```

## 4. Cài đặt và chạy

### Yêu cầu

- JDK 17, Maven (hoặc Eclipse có sẵn Maven).
- SQL Server đang chạy, đã bật TCP/IP cổng 1433.
- Database `FashionStoreDB` đã tạo đủ bảng theo ERD.

### Các bước

1. **Dữ liệu ban đầu:** chạy script chèn dữ liệu mẫu vào `FashionStoreDB`: role `ADMIN` và `CUSTOMER`; danh mục, sản phẩm, ảnh; màu, size, biến thể; tồn kho cho từng biến thể.
2. **Cấu hình:** sửa `application.properties` (mục 5). Không đưa mật khẩu và khóa bí mật lên Git.
3. **Chạy:** chuột phải `FashionStoreApplication` > Run As > Spring Boot App, hoặc dùng lệnh:
   ```bash
   mvn spring-boot:run
   ```
4. **Mở trình duyệt:** `http://localhost:8080`.
5. **Tạo tài khoản admin:** đăng ký một tài khoản trên `/register`, rồi nâng quyền bằng SQL:
   ```sql
   UPDATE [USER]
   SET roleId = (SELECT roleId FROM Role WHERE roleName = 'ADMIN')
   WHERE email = 'email-cua-ban@gmail.com';
   ```
   Đăng xuất và đăng nhập lại, menu sẽ có nút **Quản trị**.

### Thử thanh toán VNPay (sandbox)

1. Đăng ký tài khoản sandbox tại `https://sandbox.vnpayment.vn/devreg/` để nhận mã website (`vnp_TmnCode`) và chuỗi bí mật (`vnp_HashSecret`).
2. Đặt hai giá trị vào biến môi trường `VNPAY_TMN_CODE` và `VNPAY_HASH_SECRET` (Eclipse: Run Configurations > Environment).
3. Khi thanh toán dùng thẻ test do VNPay công bố trong tài liệu sandbox.
4. VNPay chỉ gọi được IPN khi website mở ra internet. Chạy ở `localhost` thì kết quả được xác nhận qua trang `/payment/vnpay/return`.

## 5. Cấu hình

`src/main/resources/application.properties` (mẫu, thay giá trị phù hợp):

```properties
# Cơ sở dữ liệu
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FashionStoreDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=MAT_KHAU_CUA_BAN

spring.jpa.hibernate.ddl-auto=none
spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
spring.thymeleaf.cache=false

# Upload ảnh sản phẩm
app.upload-dir=uploads
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=40MB

# VNPay sandbox
vnpay.tmn-code=${VNPAY_TMN_CODE:}
vnpay.hash-secret=${VNPAY_HASH_SECRET:}
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/payment/vnpay/return

# Thông tin chuyển khoản hiển thị cho khách (tùy chọn)
shop.bank.name=Vietcombank
shop.bank.account=0123456789
shop.bank.owner=TEN CHU TAI KHOAN
```

## 6. Danh sách đường dẫn (URL)

### Công khai

| URL | Mô tả |
|---|---|
| `GET /` | Trang chủ |
| `GET /category/{id}` | Sản phẩm theo danh mục |
| `GET /product/{id}` | Chi tiết sản phẩm |
| `GET, POST /login`, `/logout` | Đăng nhập, đăng xuất |
| `GET, POST /register` | Đăng ký |
| `GET /payment/vnpay/return`, `/payment/vnpay/ipn` | VNPay gọi về (xác thực bằng chữ ký) |
| `GET /uploads/**`, `/images/**`, `/css/**`, `/js/**` | Tài nguyên tĩnh |

### Cần đăng nhập

| URL | Mô tả |
|---|---|
| `GET /cart`, `POST /cart/add`, `/cart/update`, `/cart/remove` | Giỏ hàng |
| `GET, POST /checkout` | Thanh toán, đặt hàng |
| `GET /orders`, `GET /orders/{mã đơn}` | Đơn hàng của tôi |
| `POST /orders/{mã đơn}/cancel` | Khách hủy đơn (khi chờ xác nhận) |
| `GET /payment/vnpay/pay/{mã đơn}` | Chuyển sang VNPay |

### Quản trị (ADMIN)

| URL | Mô tả |
|---|---|
| `/admin/orders`, `/admin/orders/{mã}` | Danh sách, chi tiết đơn; `POST .../status`, `.../mark-paid`, `.../refund` |
| `/admin/products`, `/new`, `/{id}` | Quản lý sản phẩm; `POST /save`, `/{id}/status`, `/{id}/delete` |
| `/admin/products/{id}/variants...` | Thêm, sửa, xóa biến thể; thêm màu `/colors`, size `/sizes` |
| `/admin/products/{id}/images...` | Tải ảnh, đặt ảnh chính, xóa ảnh |
| `/admin/categories`, `/new`, `/{id}/edit` | Quản lý danh mục; `POST /save`, `/{id}/toggle`, `/{id}/delete` |
| `/admin/imports`, `/new`, `/{mã}` | Phiếu nhập; `POST .../confirm`, `.../cancel`, `.../reverse` |
| `/admin/inventory`, `/{id}/history` | Tồn kho; `POST /{id}/adjust` (kiểm kê) |

## 7. Cơ sở dữ liệu

### Bảng đang được sử dụng

`ROLE`, `USER`, `CATEGORY`, `PRODUCT`, `PRODUCT_IMAGE`, `PRODUCT_VARIANT`, `COLOR`, `SIZE`, `CART`, `CART_ITEM`, `ADDRESS`, `ORDER`, `ORDER_DETAIL`, `ORDER_STATUS_HISTORY`, `PAYMENT`, `INVENTORY`, `INVENTORY_TRANSACTION`, `IMPORT_RECEIPT`, `IMPORT_RECEIPT_DETAIL`.

Các bảng có trong ERD nhưng chưa dùng: `DISCOUNT`, `REVIEW`, `PASSWORD_RESET_TOKEN`.

### Giá trị cho phép (theo ràng buộc CHECK đã đối chiếu)

| Bảng.Cột | Giá trị |
|---|---|
| `CART.Status` | `ACTIVE`, `CONVERTED`, `ABANDONED` |
| `ORDER.OrderStatus` | `PENDING`, `CONFIRMED`, `PREPARING`, `SHIPPING`, `COMPLETED`, `CANCELLED`, `RETURNED` |
| `PAYMENT.PaymentMethod` | `COD`, `BANK_TRANSFER`, `CREDIT_CARD`, `E_WALLET` (VNPay được lưu là `E_WALLET`) |
| `PAYMENT.PaymentStatus` | `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED` |
| `INVENTORY_TRANSACTION.TransactionType` | `RELEASE`, `RESERVE`, `ADJUSTMENT`, `RETURN`, `SALE`, `IMPORT` |
| `INVENTORY_TRANSACTION.Quantity` | luôn lớn hơn 0 (hướng tăng, giảm ghi trong ghi chú `[+]`, `[-]`) |
| `INVENTORY` | `quantity >= 0`, `reservedQuantity >= 0`, `reservedQuantity <= quantity` |
| `ORDER`, `ORDER_DETAIL`, `PAYMENT` | các số tiền không âm; `Quantity > 0` |
| `PRODUCT`, `CATEGORY`, `PRODUCT_VARIANT`.`status` | `ACTIVE`, `INACTIVE` **(đối chiếu)** |
| `IMPORT_RECEIPT.status` | `DRAFT`, `CONFIRMED`, `CANCELLED` **(đối chiếu)** |

### Lưu ý về tên cột

Tên cột trong database có thể khác ERD (ví dụ `CreatedAt` và `createAt`). Mỗi entity có `@Column(name = "...")` phải khớp đúng tên cột thật. Gặp lỗi `Invalid column name` thì đối chiếu bằng:

```sql
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'TEN_BANG'
ORDER BY ORDINAL_POSITION;
```

Bảng `USER` và `ORDER` trùng từ khóa SQL nên entity khai báo `@Table(name = "\"USER\"")` và `@Table(name = "\"ORDER\"")` (class `Orders`).

## 8. Quy tắc nghiệp vụ chính

### Giỏ hàng

- Mỗi tài khoản có một giỏ `ACTIVE`; thêm lại cùng biến thể thì cộng dồn số lượng, tối đa 99 mỗi dòng.
- Từ trang chủ: sản phẩm chỉ có một biến thể thì thêm ngay; nhiều biến thể thì chuyển sang trang chi tiết để chọn màu và size.
- Chưa kiểm tra tồn kho khi thêm giỏ; kiểm tra khi đặt hàng.

### Đặt hàng (một transaction duy nhất)

1. Kiểm tra giỏ không trống, địa chỉ hợp lệ.
2. Với từng sản phẩm: còn bán, rồi giữ chỗ tồn kho bằng một câu `UPDATE` có điều kiện (`reservedQuantity += SL` nếu còn đủ hàng).
3. Tính tiền: phí vận chuyển 30.000 đ, miễn phí từ 500.000 đ.
4. Tạo đơn (`PENDING`), chi tiết đơn (lưu giá lúc mua), lịch sử trạng thái, thanh toán (`PENDING`).
5. Giỏ chuyển sang `CONVERTED`. Lỗi ở bất kỳ bước nào thì hoàn tác toàn bộ.

### Quy trình trạng thái đơn hàng

| Từ | Được chuyển sang |
|---|---|
| `PENDING` | `CONFIRMED`, `CANCELLED` |
| `CONFIRMED` | `PREPARING`, `CANCELLED` |
| `PREPARING` | `SHIPPING`, `CANCELLED` |
| `SHIPPING` | `COMPLETED`, `RETURNED` |
| `COMPLETED` | `RETURNED` |
| `CANCELLED`, `RETURNED` | kết thúc |

### Tồn kho

| Sự kiện | `quantity` | `reservedQuantity` |
|---|---|---|
| Đặt hàng | không đổi | `+SL` |
| Hủy đơn | không đổi | `-SL` |
| Chuyển sang Đang giao | `-SL` | `-SL` |
| Trả hàng | `+SL` | không đổi |
| Nhập hàng (xác nhận phiếu) | `+SL` | không đổi |
| Kiểm kê | đặt về số thực tế (không thấp hơn số đang giữ chỗ) | không đổi |

Số có thể bán bằng `quantity - reservedQuantity`; cảnh báo sắp hết khi còn từ 5 trở xuống.

### Thanh toán

- COD: thanh toán `PENDING`, tự chuyển `SUCCESS` khi đơn `COMPLETED`.
- Chuyển khoản: admin bấm "Xác nhận đã thanh toán" khi nhận tiền.
- VNPay: tạo URL có chữ ký HMAC-SHA512, kết quả về qua trang `return` và IPN song song. Hệ thống xác thực chữ ký, so khớp số tiền, bỏ qua giao dịch đã xử lý, rồi cập nhật thanh toán (`SUCCESS` hoặc `FAILED`) và đơn (`CONFIRMED`).
- Đơn đã thanh toán rồi bị hủy hoặc trả hàng: admin hoàn tiền bên ngoài hệ thống rồi bấm "Đã hoàn tiền" để chuyển `REFUNDED`.

### Nhập hàng

- Phiếu nhập có trạng thái `DRAFT`, rồi `CONFIRMED` hoặc `CANCELLED`.
- Xác nhận phiếu: cộng `quantity`, tự tạo dòng tồn kho nếu biến thể chưa có, ghi lịch sử `IMPORT`.
- Form tạo phiếu có mã dùng một lần chống gửi lặp (bấm Back rồi gửi lại, bấm đúp).
- Hoàn tác phiếu đã nhập chỉ thực hiện được nếu số hàng đó chưa bán ra.

### Hình ảnh sản phẩm

- Tối đa 10 ảnh mỗi sản phẩm, mỗi ảnh tối đa 5MB, định dạng JPG, PNG, GIF, WEBP (kiểm tra theo nội dung file).
- Ảnh tải lên lưu ở `uploads/products/` với tên ngẫu nhiên, đường dẫn trong DB dạng `/uploads/products/<mã>.jpg`. Có thể nhập đường dẫn ảnh có sẵn (ví dụ `/images/products/ao-thun.jpg`).

## 9. Bảo mật

- Mật khẩu băm BCrypt; đăng nhập bằng email (không phân biệt hoa thường); chỉ tài khoản `ACTIVE` đăng nhập được; thông báo lỗi đăng nhập chung.
- Spring Security: bắt buộc CSRF token cho mọi form POST; `/admin/**` chỉ ADMIN; `/cart`, `/checkout`, `/orders` cần đăng nhập; đăng xuất bằng POST.
- Mọi dữ liệu giỏ hàng, đơn hàng, địa chỉ được kiểm tra quyền sở hữu theo tài khoản đang đăng nhập.
- Giá và số lượng luôn tính lại ở server, không tin dữ liệu từ trình duyệt.
- Dữ liệu VNPay gửi về chỉ được tin khi chữ ký hợp lệ và số tiền khớp.
- Upload ảnh: đặt lại tên file, kiểm tra nội dung thật, giới hạn kích thước, chỉ xóa file nằm trong `uploads/`.
- Thao tác nhiều bước (đặt hàng, nhập hàng, đổi trạng thái đơn, hoàn tác) chạy trong transaction; các thao tác tranh chấp dùng khóa dòng để tránh xử lý đồng thời.

## 10. Kiểm thử và tài liệu đi kèm

- **Test plan và test case** đăng ký, đăng nhập: file Excel `FashionStore_TestCase_DangKy_DangNhap.xlsx` (68 test case, có sheet tổng quan tự tính kết quả, quy tắc nghiệp vụ, dữ liệu test và danh sách lỗi).
- **Activity diagram** đã vẽ cho: đặt hàng, thanh toán VNPay, nhập hàng, biến động tồn kho theo đơn, kiểm kê tồn kho, xử lý đơn hàng (admin), thêm vào giỏ, xem sửa xóa trong giỏ.

## 11. Hạn chế và hướng phát triển

- Chưa có: quên mật khẩu, đánh giá sản phẩm, khuyến mãi (`DISCOUNT`), trang thống kê, email thông báo.
- Hoàn tiền VNPay chưa gọi API tự động (admin hoàn thủ công rồi ghi nhận).
- Thanh toán chuyển khoản chưa tự động đối soát.
- Trang danh mục cha chưa hiển thị sản phẩm của danh mục con.
- Đăng nhập: chưa giới hạn số lần sai, chưa giữ lại email sau khi đăng nhập sai, luôn về trang chủ sau khi đăng nhập.
- Chưa có tự động hóa kiểm thử (JUnit, MockMvc, Selenium).

## 12. Lưu ý khi dùng Git

`.gitignore` nên có:

```
target/
uploads/
.settings/
.classpath
.project
.factorypath
.idea/
*.iml
HELP.md
```

- Không commit mật khẩu SQL Server và khóa VNPay; dùng biến môi trường hoặc xóa trước khi đẩy lên.
- Lưu script tạo bảng và dữ liệu mẫu thành file `.sql` (ví dụ thư mục `database/`) để người khác dựng lại database, vì dữ liệu SQL Server không nằm trên GitHub.
- Ảnh trong `static/images/` được đưa lên cùng code; ảnh trong `uploads/` thì không.
