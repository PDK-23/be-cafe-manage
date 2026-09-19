# Auth Service

Java 21 / Spring Boot 4.1.1, cổng 8080. Swagger: http://localhost:8080/swagger-ui.html.

`POST /api/auth/login`: `{ "username": "admin", "password": "..." }` trả về `{ token, user }`.

JWT HS256: issuer `cafe-flow-auth`, hạn 8 giờ, claims `roles`, `name`, `ver`. Mọi service kiểm tra chữ ký và hạn token, sau đó kiểm tra tài khoản còn hoạt động tại Auth Service. Đổi thông tin tài khoản, mật khẩu hoặc vai trò tăng `tokenVersion`, vô hiệu hóa các phiên cũ. Password được băm BCrypt; API không trả password hash.

`GET /api/auth/me`, `GET /api/auth/validate` yêu cầu phiên hợp lệ. Quản trị tài khoản: `GET/POST /api/auth/users`, `PUT /api/auth/users/{id}`; mặc định chỉ Admin. Không cho Admin tự khóa hoặc hạ quyền tài khoản đang dùng.

Profile `local` tạo ba tài khoản mẫu nếu chưa tồn tại. Ngoài `local`, đặt `CAFE_JWT_SECRET` (Base64 ít nhất 32 byte), `CAFE_ADMIN_USERNAME` và `CAFE_ADMIN_PASSWORD` (ít nhất 12 ký tự). Bootstrap Admin chỉ thực hiện khi bảng tài khoản rỗng. Dùng cùng JWT secret cho cả bốn service.

Khi Auth Service hoặc Permission Service không phản hồi, quyền truy cập bị từ chối; không bỏ qua phân quyền. Admin được bỏ qua kiểm tra RBAC nhưng vẫn phải có phiên đang hoạt động.
