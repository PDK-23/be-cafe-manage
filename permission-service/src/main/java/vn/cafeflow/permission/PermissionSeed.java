package vn.cafeflow.permission;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
class PermissionSeed {
    @Bean
    CommandLineRunner seed(Rules rules, Menus menus) {
        return args -> {
            if (rules.count() == 0 && menus.count() == 0) {
                for (String role : new String[] { "CASHIER", "MANAGER" }) {
                    for (String path : new String[] { "/api/pos/tables/**", "/api/pos/orders/**",
                            "/api/pos/customers/**", "/api/pos/invoices/**" })
                        add(rules, role, "*", path);
                    add(rules, role, "GET", "/api/pos/day");
                    add(rules, role, "GET", "/api/catalog/**");
                }
                add(rules, "MANAGER", "*", "/api/catalog/**");
                add(rules, "MANAGER", "*", "/api/pos/day/**");
                add(rules, "MANAGER", "GET", "/api/pos/reports");
            }
            if (menus.count() == 0) {
                String[][] data = { { "Bán hàng", "/ban-hang", "layout", "ADMIN,MANAGER,CASHIER" },
                        { "Thực đơn", "/thuc-don", "coffee", "ADMIN,MANAGER" },
                        { "Khách hàng", "/khach-hang", "users", "ADMIN,MANAGER,CASHIER" },
                        { "Hóa đơn", "/hoa-don", "receipt", "ADMIN,MANAGER,CASHIER" },
                        { "Báo cáo", "/bao-cao", "chart", "ADMIN,MANAGER" },
                        { "Tất toán", "/tat-toan", "wallet", "ADMIN,MANAGER" },
                        { "Nhân viên", "/nhan-vien", "staff", "ADMIN" },
                        { "Phân quyền", "/phan-quyen", "shield", "ADMIN" } };
                for (int i = 0; i < data.length; i++) {
                    var m = new MenuEntry();
                    m.label = data[i][0];
                    m.path = data[i][1];
                    m.icon = data[i][2];
                    m.roles = data[i][3];
                    m.sortOrder = i;
                    menus.save(m);
                }
            }
        };
    }

    private void add(Rules repo, String role, String method, String path) {
        var r = new EndpointRule();
        r.role = role;
        r.method = method;
        r.path = path;
        repo.save(r);
    }
}
