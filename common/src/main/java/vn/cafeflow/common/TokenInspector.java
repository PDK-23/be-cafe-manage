package vn.cafeflow.common;

import org.springframework.security.oauth2.jwt.Jwt;

public interface TokenInspector {
    boolean valid(Jwt token);
}
