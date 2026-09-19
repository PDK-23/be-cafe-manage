# Cafe Flow · Backend

Java 21, Spring Boot 4.1.1, JPA, Bean Validation, Spring Security và springdoc OpenAPI. Maven reactor: `common`, `auth-service`, `catalog-service`, `pos-service`, `permission-service`.

```powershell
.\mvnw.cmd package
.\start-local.ps1 -Demo
```

Demo dùng H2 trong bộ nhớ; bỏ `-Demo` để dùng MySQL sau khi đặt `DB_USERNAME`/`DB_PASSWORD`. Dừng bằng `stop-local.ps1`. Log ở `.logs/`. `mvnw.cmd test` chạy JUnit/H2.

Các cổng: Auth 8080, Catalog 8081, POS 8082, Permission 8083. Swagger UI: `/swagger-ui.html` trên từng cổng.

Xem [README chính](../README.md) cho cấu hình MySQL, tài khoản, nghiệp vụ và giới hạn triển khai.
