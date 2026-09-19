package vn.cafeflow.permission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "vn.cafeflow.permission", "vn.cafeflow.common" })
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}