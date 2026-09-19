package vn.cafeflow.pos;

import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class Model {
    @Entity
    @Table(name = "cafe_tables")
    public static class CafeTable {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        public String name;
        public String area;
        public int seats = 4;
    }

    @Entity
    @Table(name = "customers")
    public static class Customer {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        public String name;
        @Column(unique = true)
        public String phone;
        public String email;
        public String note;
    }

    @Embeddable
    public static class Line {
        public Long productId;
        public String name;
        public int quantity;
        @Column(precision = 14, scale = 0)
        public BigDecimal price;
        public String note = "";

        public Line() {
        }

        public Line(Long id, String name, int quantity, BigDecimal price) {
            this.productId = id;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }
    }

    @Entity
    @Table(name = "cafe_orders")
    public static class Order {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @Version
        public long version;
        public Long tableId;
        public String tableName;
        public Long dayId;
        public Long customerId;
        public String customerName;
        public String status = "OPEN";
        public String note = "";
        public String cancelReason;
        public Instant createdAt = Instant.now();
        public Instant paidAt;
        public String paymentMethod;
        public String cashier;
        @Column(precision = 14, scale = 0)
        public BigDecimal discount = BigDecimal.ZERO;
        @Column(precision = 14, scale = 0)
        public BigDecimal surcharge = BigDecimal.ZERO;
        @Column(precision = 14, scale = 0)
        public BigDecimal tendered = BigDecimal.ZERO;
        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
        @OrderColumn(name = "line_index")
        public List<Line> items = new ArrayList<>();

        public BigDecimal getSubtotal() {
            return items.stream().map(x -> x.price.multiply(BigDecimal.valueOf(x.quantity))).reduce(BigDecimal.ZERO,
                    BigDecimal::add);
        }

        public BigDecimal getTotal() {
            return getSubtotal().subtract(discount).add(surcharge).max(BigDecimal.ZERO);
        }

        public BigDecimal getChange() {
            return tendered.subtract(getTotal()).max(BigDecimal.ZERO);
        }
    }

    @Entity
    @Table(name = "business_days")
    public static class BusinessDay {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        public Instant openedAt = Instant.now();
        public Instant closedAt;
        public String closedBy;
        public BigDecimal openingCash = BigDecimal.ZERO;
        public BigDecimal countedCash;
        public BigDecimal expectedCash;
        public String note;
    }

    @Entity
    @Table(name = "register_state")
    public static class RegisterState {
        @Id
        public Long id = 1L;
        public Long dayId;
    }
}
