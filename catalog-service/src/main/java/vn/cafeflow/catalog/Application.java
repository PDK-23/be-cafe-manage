package vn.cafeflow.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "vn.cafeflow.catalog", "vn.cafeflow.common" })
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}