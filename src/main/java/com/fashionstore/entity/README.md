# Java Entity từ ERD

- Package: `com.example.warehouse.entity`
- Công nghệ: Jakarta Persistence (JPA) + Lombok.
- Mỗi bảng được ánh xạ bằng `@Entity`, `@Table`; khóa chính dùng `@Id` và `@GeneratedValue`.
- Khóa ngoại hiện được biểu diễn bằng trường ID (ví dụ `userId`, `productId`) để có thể dùng ngay với schema trong sơ đồ mà không cần cấu hình quan hệ object.

## Dependencies (Maven)
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <optional>true</optional>
</dependency>
```

Lưu ý: tên bảng/cột và kiểu dữ liệu được suy ra từ ảnh ERD có độ phân giải hạn chế; hãy đối chiếu lại với database thực tế trước khi chạy migration hoặc dùng `ddl-auto=validate`.
