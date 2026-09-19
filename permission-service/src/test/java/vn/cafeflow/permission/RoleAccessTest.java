package vn.cafeflow.permission;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest @ActiveProfiles({"local", "demo"}) @Transactional
class RoleAccessTest {
    @Autowired RoleAccessController roles;
    @Autowired PermissionController permissions;
    @Autowired Menus menus;

    @Test void grantsAndRevokesApiAndMenuTogetherWithoutChangingOtherRole() {
        roles.save("CASHIER", new RoleAccessController.RoleInput(Set.of("sales", "reports")));
        assertTrue(permissions.allowed(List.of("CASHIER"), "GET", "/api/pos/reports"));
        assertTrue(menus.findAll().stream().anyMatch(m -> m.path.equals("/bao-cao") && m.roles.contains("CASHIER")));
        assertFalse(permissions.allowed(List.of("CASHIER"), "DELETE", "/api/pos/customers/1"));
        assertTrue(permissions.allowed(List.of("CASHIER"), "GET", "/api/pos/customers"));
        assertTrue(permissions.allowed(List.of("MANAGER"), "POST", "/api/catalog/products"));
        assertTrue(roles.read("CASHIER").permissions().stream().filter(f -> f.id().equals("reports")).findFirst().orElseThrow().enabled());
        roles.save("CASHIER", new RoleAccessController.RoleInput(Set.of("sales")));
        assertFalse(permissions.allowed(List.of("CASHIER"), "GET", "/api/pos/reports"));
        assertFalse(menus.findAll().stream().anyMatch(m -> m.path.equals("/bao-cao") && m.roles.contains("CASHIER")));
        assertTrue(permissions.allowed(List.of("ADMIN"), "GET", "/api/pos/reports"));
    }

    @Test void emptySelectionRevokesEverythingAndAdminCannotBeChanged() {
        roles.save("CASHIER", new RoleAccessController.RoleInput(Set.of()));
        assertFalse(permissions.allowed(List.of("CASHIER"), "POST", "/api/pos/orders/1/checkout"));
        assertTrue(menus.findAll().stream().noneMatch(m -> m.roles.contains("CASHIER")));
        assertThrows(ResponseStatusException.class, () -> roles.save("ADMIN", new RoleAccessController.RoleInput(Set.of())));
        assertThrows(ResponseStatusException.class, () -> roles.save("MANAGER", new RoleAccessController.RoleInput(Set.of("unknown"))));
    }
}
