package vn.cafeflow.auth;

import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.cafeflow.common.TokenInspector;

@Component
public class AccountTokenInspector implements TokenInspector {
    private final Accounts accounts;

    public AccountTokenInspector(Accounts accounts) {
        this.accounts = accounts;
    }

    public boolean valid(Jwt token) {
        return accounts.findByUsername(token.getSubject())
                .filter(a -> a.enabled && token.getClaimAsStringList("roles").contains(a.role)
                        && token.hasClaim("ver") && ((Number) token.getClaim("ver")).longValue() == a.tokenVersion)
                .isPresent();
    }
}
