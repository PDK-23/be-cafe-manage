package vn.cafeflow.permission;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import vn.cafeflow.common.ApiErrors;

/** Business permissions and their menu entries are saved in one transaction. */
@RestController
@RequestMapping("/api/permissions/roles")
public class RoleAccessController {
    private final Rules rules;
    private final Menus menus;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public RoleAccessController(Rules rules, Menus menus) {
        this.rules = rules;
        this.menus = menus;
    }

    record Grant(String method, String path) {}
    record Feature(String id, String label, String description, String menuPath, String icon, List<Grant> grants) {}
    public record FeatureView(String id, String label, String description, boolean enabled) {}
    public record RoleView(String role, List<FeatureView> permissions) {}
    public record RoleInput(@NotNull Set<String> permissions) {}

    private static Grant grant(String method, String path) { return new Grant(method, path); }
    private static final List<Feature> FEATURES = List.of(
        new Feature("sales", "Bán hàng", "Gọi món, sửa đơn, chuyển/gộp bàn, hủy và thanh toán.", "/ban-hang", "layout", List.of(
            grant("GET", "/api/pos/tables/**"), grant("POST", "/api/pos/tables/*/items"),
            grant("*", "/api/pos/orders/**"), grant("GET", "/api/catalog/**"),
            grant("GET", "/api/pos/customers"), grant("GET", "/api/pos/day"))),
        new Feature("catalog", "Quản lý thực đơn", "Thêm, sửa, xóa danh mục và món; cập nhật giá bán.", "/thuc-don", "coffee", List.of(
            grant("*", "/api/catalog/**"))),
        new Feature("customers", "Quản lý khách hàng", "Tra cứu, thêm, sửa và xóa thông tin khách hàng.", "/khach-hang", "users", List.of(
            grant("*", "/api/pos/customers/**"))),
        new Feature("invoices", "Tra cứu hóa đơn", "Xem lịch sử, chi tiết và in lại hóa đơn đã thanh toán.", "/hoa-don", "receipt", List.of(
            grant("GET", "/api/pos/invoices/**"))),
        new Feature("reports", "Báo cáo doanh thu", "Xem doanh thu, món bán chạy, khách hàng và xuất báo cáo.", "/bao-cao", "chart", List.of(
            grant("GET", "/api/pos/reports"))),
        new Feature("settlement", "Tất toán ca", "Đối soát tiền mặt, đóng ca và mở ca kinh doanh mới.", "/tat-toan", "wallet", List.of(
            grant("*", "/api/pos/day/**")))
    );

    private void validateRole(String role) {
        if (!Set.of("CASHIER", "MANAGER").contains(role))
            throw ApiErrors.bad("Chỉ cấp quyền cho Thu ngân hoặc Quản lý. Admin luôn có toàn quyền.");
    }

    @GetMapping("/{role}")
    @Transactional(readOnly = true)
    public RoleView read(@PathVariable String role) {
        validateRole(role);
        var existing = rules.findAll().stream().filter(r -> role.equals(r.role)).toList();
        var entries = menus.findAll();
        return new RoleView(role, FEATURES.stream().map(f -> new FeatureView(f.id(), f.label(), f.description(),
            f.grants().stream().allMatch(g -> existing.stream().anyMatch(r -> covers(r, g))) &&
            entries.stream().anyMatch(m -> f.menuPath().equals(m.path) && hasRole(m, role)))).toList());
    }

    private boolean covers(EndpointRule rule, Grant grant) {
        if (!rule.method.equals("*") && !rule.method.equals(grant.method())) return false;
        if (rule.path.equals(grant.path())) return true;
        // A rule limited to one endpoint cannot imply access to a whole endpoint family.
        if (grant.path().contains("*") && !rule.path.endsWith("/**")) return false;
        return matcher.match(rule.path, grant.path().replace("**", "1").replace("*", "1")) &&
            (!grant.path().endsWith("/**") || matcher.match(rule.path, grant.path().substring(0, grant.path().length() - 3)));
    }

    private boolean hasRole(MenuEntry menu, String role) {
        return Arrays.asList(menu.roles.split(",")).contains(role);
    }

    @PutMapping("/{role}")
    @Transactional
    public RoleView save(@PathVariable String role, @Valid @RequestBody RoleInput input) {
        validateRole(role);
        var known = FEATURES.stream().map(Feature::id).toList();
        if (!known.containsAll(input.permissions())) throw ApiErrors.bad("Chức năng được chọn không hợp lệ");

        // Replace this role's policy so broad legacy wildcards cannot override an unchecked permission.
        rules.deleteAll(rules.findAll().stream().filter(r -> role.equals(r.role)).toList());
        var grants = FEATURES.stream().filter(f -> input.permissions().contains(f.id()))
            .flatMap(f -> f.grants().stream()).distinct().toList();
        for (var grant : grants) {
            var rule = new EndpointRule();
            rule.role = role; rule.method = grant.method(); rule.path = grant.path(); rules.save(rule);
        }
        var enabledPaths = FEATURES.stream().filter(f -> input.permissions().contains(f.id())).map(Feature::menuPath).toList();
        var entries = menus.findAll();
        for (var entry : entries) {
            var roles = new LinkedHashSet<>(Arrays.asList(entry.roles.split(",")));
            roles.remove(role);
            roles.add("ADMIN");
            if (enabledPaths.contains(entry.path)) roles.add(role);
            entry.roles = String.join(",", roles);
            menus.save(entry);
        }
        for (var feature : FEATURES) {
            if (!enabledPaths.contains(feature.menuPath()) || entries.stream().anyMatch(m -> feature.menuPath().equals(m.path))) continue;
            var entry = new MenuEntry(); entry.label = feature.label(); entry.path = feature.menuPath();
            entry.icon = feature.icon(); entry.roles = "ADMIN," + role; entry.sortOrder = FEATURES.indexOf(feature);
            menus.save(entry);
        }
        rules.flush(); menus.flush();
        return read(role);
    }
}
