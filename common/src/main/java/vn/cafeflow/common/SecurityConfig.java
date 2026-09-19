package vn.cafeflow.common;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    public static final String ISSUER = "cafe-flow-auth";

    @Bean
    SecretKey secretKey(Environment env, @Value("${cafe.jwt.secret:}") String configured) {
        if (configured.isBlank() && Arrays.asList(env.getActiveProfiles()).contains("local"))
            configured = Base64.getEncoder()
                    .encodeToString("cafe-flow-local-development-key-32bytes-only".getBytes(StandardCharsets.UTF_8));
        if (configured.isBlank())
            throw new IllegalStateException("CAFE_JWT_SECRET is required outside profile local");
        byte[] bytes = Base64.getDecoder().decode(configured);
        if (bytes.length < 32)
            throw new IllegalStateException("CAFE_JWT_SECRET must decode to at least 32 bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtDecoder decoder(SecretKey key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectProvider<TokenInspector> inspector,
            @Value("${spring.application.name}") String name,
            @Value("${cafe.auth.url:http://localhost:8080}") String authUrl,
            @Value("${cafe.permission.url:http://localhost:8083}") String permissionUrl) throws Exception {
        var converter = new JwtAuthenticationConverter();
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        converter.setJwtGrantedAuthoritiesConverter(roles);
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/login", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/error")
                        .permitAll()
                        .anyRequest().access((supplier, context) -> {
                            var auth = supplier.get();
                            if (!(auth instanceof JwtAuthenticationToken jwt))
                                return new AuthorizationDecision(false);
                            try {
                                var local = inspector.getIfAvailable();
                                if (local != null) {
                                    if (!local.valid(jwt.getToken()))
                                        return new AuthorizationDecision(false);
                                } else {
                                    var request = HttpRequest.newBuilder(URI.create(authUrl + "/api/auth/validate"))
                                            .timeout(Duration.ofSeconds(3))
                                            .header("Authorization", "Bearer " + jwt.getToken().getTokenValue()).GET()
                                            .build();
                                    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                                    if (response.statusCode() != 200 || !response.body().trim().equals("true"))
                                        return new AuthorizationDecision(false);
                                }
                            } catch (Exception ex) {
                                return new AuthorizationDecision(false);
                            }
                            if (auth.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN")))
                                return new AuthorizationDecision(true);
                            String path = context.getRequest().getRequestURI();
                            if ((name.equals("auth-service")
                                    && (path.equals("/api/auth/me") || path.equals("/api/auth/validate")))
                                    || (name.equals("permission-service") &&
                                            (path.equals("/api/permissions/check")
                                                    || path.equals("/api/permissions/menus"))))
                                return new AuthorizationDecision(true);
                            if (name.equals("permission-service"))
                                return new AuthorizationDecision(false);
                            try {
                                var uri = URI.create(permissionUrl + "/api/permissions/check?method="
                                        + context.getRequest().getMethod() + "&path="
                                        + URLEncoder.encode(path, StandardCharsets.UTF_8));
                                var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3))
                                        .header("Authorization", "Bearer " + jwt.getToken().getTokenValue()).GET()
                                        .build();
                                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                                return new AuthorizationDecision(
                                        response.statusCode() == 200 && response.body().trim().equals("true"));
                            } catch (Exception ex) {
                                return new AuthorizationDecision(false);
                            }
                        }))
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)));
        return http.build();
    }
}
