package vn.cafeflow.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public String name;
    public Long categoryId;
    @Column(precision = 14, scale = 0)
    public BigDecimal price;
    public boolean available = true;
    public String icon;
}
