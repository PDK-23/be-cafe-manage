package vn.cafeflow.permission;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import vn.cafeflow.common.ApiErrors;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {
    private final Rules rules;
    private final Menus menus;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public PermissionController(Rules r, Menus m) {
        rules = r;
        menus = m;
    }

    @GetMapping("/check")
    public boolean check(@AuthenticationPrincipal Jwt jwt, @RequestParam String method, @RequestParam String path) {
        return allowed(jwt.getClaimAsStringList("roles"), method, path);
    }

    boolean allowed(List<String> roles, String method, String path) {
        return roles.contains("ADMIN") || rules.findAll().stream().anyMatch(r -> roles.contains(r.role)
                && (r.method.equals("*") || r.method.equals(method)) && matcher.match(r.path, path));
    }

    @GetMapping("/menus")
    public List<MenuEntry> visible(@AuthenticationPrincipal Jwt jwt) {
        var roles = jwt.getClaimAsStringList("roles");
        return menus.findAll().stream()
                .filter(m -> roles.contains("ADMIN") || Arrays.stream(m.roles.split(",")).anyMatch(roles::contains))
                .sorted(Comparator.comparingInt(m -> m.sortOrder)).toList();
    }

    @GetMapping("/rules")
    public List<EndpointRule> rules() {
        return rules.findAll();
    }

    public record RuleInput(@NotNull @Pattern(regexp = "CASHIER|MANAGER|ADMIN") String role,
            @NotNull @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH|\\*") String method,
            @NotBlank @Pattern(regexp = "/api/.*") String path) {
    }

    @PostMapping("/rules")
    public EndpointRule create(@Valid @RequestBody RuleInput i) {
        var r = new EndpointRule();
        r.role = i.role();
        r.method = i.method();
        r.path = i.path();
        return rules.save(r);
    }

    @DeleteMapping("/rules/{id}")
    public void delete(@PathVariable Long id) {
        rules.deleteById(id);
    }

    public record MenuInput(@NotBlank @Size(max = 80) String label,
            @NotBlank @Pattern(regexp = "/[a-z0-9/-]*") String path, @NotBlank String icon,
            @NotBlank @Pattern(regexp = "(ADMIN|MANAGER|CASHIER)(,(ADMIN|MANAGER|CASHIER))*") String roles,
            Long parentId, int sortOrder) {
    }

    @PostMapping("/menus")
    public MenuEntry createMenu(@Valid @RequestBody MenuInput i) {
        return saveMenu(new MenuEntry(), i);
    }

    @PutMapping("/menus/{id}")
    public MenuEntry updateMenu(@PathVariable Long id, @Valid @RequestBody MenuInput i) {
        return saveMenu(menus.findById(id).orElseThrow(ApiErrors::missing), i);
    }

    @DeleteMapping("/menus/{id}")
    public void deleteMenu(@PathVariable Long id) {
        if (menus.findAll().stream().anyMatch(m -> id.equals(m.parentId)))
            throw ApiErrors.bad("Hãy xóa menu con trước");
        menus.deleteById(id);
    }

    private MenuEntry saveMenu(MenuEntry m, MenuInput i) {
        if (i.parentId() != null) {
            var parent = menus.findById(i.parentId()).orElseThrow(ApiErrors::missing);
            if (parent.parentId != null || i.parentId().equals(m.id))
                throw ApiErrors.bad("Menu hỗ trợ tối đa hai cấp");
            if (m.id != null && menus.findAll().stream().anyMatch(x -> m.id.equals(x.parentId)))
                throw ApiErrors.bad("Menu có menu con không thể làm menu cấp hai");
        }
        m.label = i.label();
        m.path = i.path();
        m.icon = i.icon();
        m.roles = i.roles();
        m.parentId = i.parentId();
        m.sortOrder = i.sortOrder();
        return menus.save(m);
    }
}
