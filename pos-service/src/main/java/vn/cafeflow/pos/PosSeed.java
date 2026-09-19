package vn.cafeflow.pos;

import static vn.cafeflow.pos.Model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PosSeed implements CommandLineRunner {
    @PersistenceContext
    EntityManager em;
    private final Environment env;

    public PosSeed(Environment env) {
        this.env = env;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (em.find(RegisterState.class, 1L) != null)
            return;
        var day = new BusinessDay();
        em.persist(day);
        var register = new RegisterState();
        register.dayId = day.id;
        em.persist(register);
        List<CafeTable> tables = new ArrayList<>();
        String[] areas = { "Khu vực VIP", "Sân vườn", "Tầng trệt" };
        String[] labels = { "VIP ", "Sân vườn ", "Bàn " };
        int[] counts = { 6, 6, 15 };
        for (int a = 0; a < areas.length; a++)
            for (int n = 1; n <= counts[a]; n++) {
                var t = new CafeTable();
                t.name = labels[a] + n;
                t.area = areas[a];
                t.seats = a == 0 ? 6 : 4;
                em.persist(t);
                tables.add(t);
            }
        if (!Arrays.asList(env.getActiveProfiles()).contains("local"))
            return;
        var customer = new Customer();
        customer.name = "Nguyễn Minh Anh";
        customer.phone = "0985347104";
        customer.email = "minhanh@example.com";
        customer.note = "Khách quen • thích ít đường";
        em.persist(customer);
        var garden = new Order();
        garden.tableId = tables.get(7).id;
        garden.tableName = tables.get(7).name;
        garden.dayId = day.id;
        garden.customerId = customer.id;
        garden.customerName = customer.name;
        garden.items.add(new Line(3L, "Bạc xỉu", 1, BigDecimal.valueOf(25000)));
        garden.items.add(new Line(5L, "Cold Brew", 1, BigDecimal.valueOf(45000)));
        garden.items.add(new Line(6L, "Trà sữa truyền thống", 1, BigDecimal.valueOf(30000)));
        em.persist(garden);
        var vip = new Order();
        vip.tableId = tables.get(4).id;
        vip.tableName = tables.get(4).name;
        vip.dayId = day.id;
        vip.items.add(new Line(6L, "Trà sữa truyền thống", 1, BigDecimal.valueOf(30000)));
        em.persist(vip);
        var history = new Order();
        history.tableId = tables.get(12).id;
        history.tableName = tables.get(12).name;
        history.dayId = day.id;
        history.customerId = customer.id;
        history.customerName = customer.name;
        history.items.add(new Line(2L, "Cà phê sữa", 2, BigDecimal.valueOf(25000)));
        history.status = "PAID";
        history.paidAt = Instant.now().minusSeconds(3600);
        history.paymentMethod = "CASH";
        history.tendered = BigDecimal.valueOf(100000);
        history.cashier = "cashier";
        em.persist(history);
    }
}
