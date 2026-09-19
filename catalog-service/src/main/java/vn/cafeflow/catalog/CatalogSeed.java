package vn.cafeflow.catalog;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import java.math.BigDecimal;

@Configuration
@Profile("local")
class CatalogSeed {
    @Bean
    CommandLineRunner seed(Categories c, Products p) {
        return args -> {
            if (c.count() > 0)
                return;
            String[] names = { "Cà phê", "Trà sữa", "Sinh tố", "Nước ép", "Trà trái cây", "Đá xay", "Nước đóng chai",
                    "Bánh ngọt", "Ăn vặt" };
            String[][] items = { { "Cà phê đen", "Cà phê sữa", "Bạc xỉu", "Cà phê trứng", "Cold Brew" },
                    { "Trà sữa truyền thống", "Trà sữa matcha", "Trà sữa khoai môn", "Trà sữa nướng",
                            "Trà sữa trân châu đường đen" },
                    { "Sinh tố bơ", "Sinh tố xoài", "Sinh tố dâu" }, { "Nước cam", "Nước ép dưa hấu", "Nước ép táo" },
                    { "Trà đào cam sả", "Trà vải", "Trà chanh" }, { "Matcha đá xay", "Chocolate đá xay" },
                    { "Nước suối", "Coca Cola" }, { "Tiramisu", "Bánh croissant" },
                    { "Khoai tây chiên", "Hạt hướng dương" } };
            int[][] prices = { { 20000, 25000, 25000, 40000, 45000 }, { 30000, 35000, 35000, 40000, 40000 },
                    { 35000, 35000, 40000 }, { 25000, 30000, 35000 }, { 35000, 35000, 25000 }, { 45000, 45000 },
                    { 10000, 15000 }, { 45000, 35000 }, { 30000, 20000 } };
            String[] icons = { "coffee", "milk", "blend", "citrus", "leaf", "snowflake", "bottle", "cake", "cookie" };
            for (int a = 0; a < names.length; a++) {
                var cat = new Category();
                cat.name = names[a];
                c.save(cat);
                for (int b = 0; b < items[a].length; b++) {
                    var item = new Product();
                    item.name = items[a][b];
                    item.price = BigDecimal.valueOf(prices[a][b]);
                    item.categoryId = cat.id;
                    item.icon = icons[a];
                    p.save(item);
                }
            }
        };
    }
}
