package vn.cafeflow.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Arrays;

@Configuration
class AuthSeed {
    @Bean
    CommandLineRunner seed(Accounts repo, PasswordEncoder encoder, Environment env) {
        return args -> {
            if (Arrays.asList(env.getActiveProfiles()).contains("local")) {
                add(repo, encoder, "admin", "Quản trị viên", "ADMIN", "Cafe@Admin2026");
                add(repo, encoder, "manager", "Quản lý", "MANAGER", "Cafe@Manager2026");
                add(repo, encoder, "cashier", "Thu ngân", "CASHIER", "Cafe@Cashier2026");
            } else if (repo.count() == 0) {
                String password = env.getProperty("CAFE_ADMIN_PASSWORD", "");
                if (password.length() < 12)
                    throw new IllegalStateException(
                            "Set CAFE_ADMIN_PASSWORD (at least 12 characters) to bootstrap admin");
                add(repo, encoder, env.getProperty("CAFE_ADMIN_USERNAME", "admin"), "Quản trị viên", "ADMIN", password);
            }
        };
    }

    private void add(Accounts repo, PasswordEncoder encoder, String username, String name, String role,
            String password) {
        if (repo.findByUsername(username).isPresent())
            return;
        var a = new Account();
        a.username = username;
        a.name = name;
        a.role = role;
        a.passwordHash = encoder.encode(password);
        repo.save(a);
    }
}
