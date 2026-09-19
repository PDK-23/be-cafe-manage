package vn.cafeflow.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKey;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.cafeflow.common.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final Accounts accounts;
    private final PasswordEncoder encoder;
    private final SecretKey key;

    public AuthController(Accounts accounts, PasswordEncoder encoder, SecretKey key) {
        this.accounts = accounts;
        this.encoder = encoder;
        this.key = key;
    }

    public record Login(@NotBlank String username, @NotBlank String password) {
    }

    public record User(Long id, String username, String name, String role, boolean enabled) {
    }

    public record Input(@NotBlank @Size(max = 60) @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
            @NotBlank @Size(max = 100) String name,
            @Pattern(regexp = "ADMIN|MANAGER|CASHIER") @NotNull String role, boolean enabled,
            @Size(max = 72) String password) {
    }

    private User view(Account a) {
        return new User(a.id, a.username, a.name, a.role, a.enabled);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody Login input) throws JOSEException {
        var a = accounts.findByUsername(input.username()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng"));
        if (!a.enabled || !encoder.matches(input.password(), a.passwordHash))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng");
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(SecurityConfig.ISSUER).subject(a.username)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(28800))).claim("roles", List.of(a.role)).claim("name", a.name)
                .claim("ver", a.tokenVersion).jwtID(UUID.randomUUID().toString()).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(key.getEncoded()));
        return Map.of("token", jwt.serialize(), "user", view(a));
    }

    @GetMapping("/me")
    public User me(@AuthenticationPrincipal Jwt jwt) {
        return view(accounts.findByUsername(jwt.getSubject()).filter(a -> a.enabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }

    @GetMapping("/validate")
    public boolean validateToken() {
        return true;
    }

    @GetMapping("/users")
    public List<User> users() {
        return accounts.findAll().stream().map(this::view).toList();
    }

    @PostMapping("/users")
    @Transactional
    public User create(@Valid @RequestBody Input input) {
        return save(new Account(), input);
    }

    @PutMapping("/users/{id}")
    @Transactional
    public User update(@PathVariable Long id, @Valid @RequestBody Input input, @AuthenticationPrincipal Jwt jwt) {
        var a = accounts.findById(id).orElseThrow(ApiErrors::missing);
        if (a.username.equals(jwt.getSubject()) && (!input.enabled() || !input.role().equals("ADMIN")))
            throw ApiErrors.bad("Không thể tự khóa hoặc hạ quyền tài khoản đang đăng nhập");
        return save(a, input);
    }

    private User save(Account a, Input i) {
        if (a.id != null)
            a.tokenVersion++;
        if (a.id == null && (i.password() == null || i.password().length() < 8))
            throw ApiErrors.bad("Mật khẩu cần ít nhất 8 ký tự");
        if (i.password() != null && !i.password().isBlank()) {
            if (i.password().length() < 8)
                throw ApiErrors.bad("Mật khẩu cần ít nhất 8 ký tự");
            a.passwordHash = encoder.encode(i.password());
        }
        a.username = i.username();
        a.name = i.name();
        a.role = i.role();
        a.enabled = i.enabled();
        return view(accounts.save(a));
    }
}
