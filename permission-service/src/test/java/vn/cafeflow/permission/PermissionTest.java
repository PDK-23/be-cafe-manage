package vn.cafeflow.permission;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.ActiveProfiles;
@SpringBootTest @ActiveProfiles({"local","demo"}) class PermissionTest {
    @Autowired PermissionController permissions;
    @Test void rolesEnforceEndpointAccess(){
        assertTrue(permissions.allowed(List.of("ADMIN"),"DELETE","/api/anything"));
        assertTrue(permissions.allowed(List.of("CASHIER"),"POST","/api/pos/orders/1/checkout"));
        assertTrue(permissions.allowed(List.of("CASHIER"),"GET","/api/catalog/products"));
        assertFalse(permissions.allowed(List.of("CASHIER"),"POST","/api/catalog/products"));
        assertFalse(permissions.allowed(List.of("CASHIER"),"GET","/api/pos/reports"));
        assertTrue(permissions.allowed(List.of("MANAGER"),"POST","/api/pos/day/close"));
        assertFalse(permissions.allowed(List.of("MANAGER"),"GET","/api/auth/users"));
    }
}
