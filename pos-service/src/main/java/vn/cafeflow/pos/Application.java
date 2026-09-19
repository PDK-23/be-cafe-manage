package vn.cafeflow.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "vn.cafeflow.pos", "vn.cafeflow.common" })
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}