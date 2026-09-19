package vn.cafeflow.pos;

import static vn.cafeflow.pos.Model.*;
import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.cafeflow.common.ApiErrors;

@Service
@Transactional
public class PosService {
    @PersistenceContext
    EntityManager em;

    private RegisterState lock() {
        var s = em.find(RegisterState.class, 1L, LockModeType.PESSIMISTIC_WRITE);
        if (s == null)
            throw ApiErrors.bad("Chưa khởi tạo quầy");
        return s;
    }

    private BusinessDay activeDay() {
        var s = lock();
        var d = em.find(BusinessDay.class, s.dayId);
        if (d.closedAt != null)
            throw ApiErrors.bad("Ngày kinh doanh đã đóng. Hãy mở ca mới.");
        return d;
    }

    public <T> T get(Class<T> type, Long id) {
        var x = em.find(type, id);
        if (x == null)
            throw ApiErrors.missing();
        return x;
    }

    public <T> List<T> all(Class<T> type, String entity) {
        return em.createQuery("from " + entity, type).getResultList();
    }

    private Order open(Long id) {
        activeDay();
        var o = get(Order.class, id);
        if (!o.status.equals("OPEN"))
            throw ApiErrors.bad("Đơn đã thanh toán, chuyển bàn hoặc hủy");
        return o;
    }

    private Order at(Long tableId) {
        return em.createQuery("from Model$Order where tableId=:id and status='OPEN'", Order.class)
                .setParameter("id", tableId).getResultStream().findFirst().orElse(null);
    }

    public record TableView(Long id, String name, String area, int seats, Order order) {
    }

    @Transactional(readOnly = true)
    public List<TableView> tables() {
        return all(CafeTable.class, "Model$CafeTable").stream()
                .map(t -> new TableView(t.id, t.name, t.area, t.seats, at(t.id))).toList();
    }

    public Order add(Long tableId, Long productId, String name, BigDecimal price) {
        var day = activeDay();
        var table = get(CafeTable.class, tableId);
        var o = at(tableId);
        if (o == null) {
            o = new Order();
            o.tableId = tableId;
            o.tableName = table.name;
            o.dayId = day.id;
            em.persist(o);
        }
        var line = o.items.stream().filter(l -> l.productId.equals(productId)).findFirst().orElse(null);
        if (line == null)
            o.items.add(new Line(productId, name, 1, price));
        else {
            if (line.quantity >= 999)
                throw ApiErrors.bad("Tối đa 999 phần mỗi món");
            line.quantity++;
        }
        return o;
    }

    public Order line(Long id, Long productId, int quantity, String note) {
        var o = open(id);
        var l = o.items.stream().filter(x -> x.productId.equals(productId)).findFirst().orElseThrow(ApiErrors::missing);
        if (quantity == 0)
            o.items.remove(l);
        else {
            l.quantity = quantity;
            l.note = note;
        }
        o.discount = o.discount.min(o.getSubtotal());
        return o;
    }

    public Order adjust(Long id, Long customerId, BigDecimal discount, BigDecimal surcharge, String note) {
        var o = open(id);
        if (discount.compareTo(o.getSubtotal()) > 0)
            throw ApiErrors.bad("Giảm giá không thể lớn hơn tiền hàng");
        o.customerId = customerId;
        o.customerName = customerId == null ? null : get(Customer.class, customerId).name;
        o.discount = discount;
        o.surcharge = surcharge;
        o.note = note;
        return o;
    }

    public Order move(Long id, Long targetId) {
        var source = open(id);
        if (source.tableId.equals(targetId))
            throw ApiErrors.bad("Hãy chọn bàn khác");
        var table = get(CafeTable.class, targetId);
        var target = at(targetId);
        if (target == null) {
            source.tableId = targetId;
            source.tableName = table.name;
            return source;
        }
        for (var l : source.items) {
            var existing = target.items.stream().filter(x -> x.productId.equals(l.productId)).findFirst().orElse(null);
            if (existing == null) {
                var copy = new Line(l.productId, l.name, l.quantity, l.price);
                copy.note = l.note;
                target.items.add(copy);
            } else {
                if (existing.price.compareTo(l.price) != 0)
                    throw ApiErrors.bad("Hai đơn có giá món khác nhau, hãy thanh toán riêng");
                if (existing.quantity + l.quantity > 999)
                    throw ApiErrors.bad("Số lượng sau gộp vượt 999");
                existing.quantity += l.quantity;
                if (!l.note.isBlank())
                    existing.note = (existing.note + "; " + l.note).replaceFirst("^; ", "");
            }
        }
        target.discount = target.discount.add(source.discount);
        target.surcharge = target.surcharge.add(source.surcharge);
        if (target.customerId == null) {
            target.customerId = source.customerId;
            target.customerName = source.customerName;
        }
        if (!source.note.isBlank())
            target.note = (target.note + "; " + source.note).replaceFirst("^; ", "");
        source.status = "MERGED";
        return target;
    }

    public Order checkout(Long id, String method, BigDecimal tendered, String cashier) {
        var o = open(id);
        if (o.items.isEmpty())
            throw ApiErrors.bad("Đơn chưa có món");
        if (method.equals("CASH") && tendered.compareTo(o.getTotal()) < 0)
            throw ApiErrors.bad("Tiền khách đưa chưa đủ");
        o.paymentMethod = method;
        o.tendered = method.equals("CASH") ? tendered : o.getTotal();
        o.cashier = cashier;
        o.status = "PAID";
        o.paidAt = Instant.now();
        return o;
    }

    public Order checkoutVersioned(Long id, long version, String method, BigDecimal tendered, String cashier) {
        var order = open(id);
        if (order.version != version)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Đơn vừa được thay đổi ở thiết bị khác. Vui lòng tải lại và kiểm tra tổng tiền.");
        return checkout(id, method, tendered, cashier);
    }

    public void cancel(Long id, String reason) {
        var o = open(id);
        o.status = "CANCELLED";
        o.cancelReason = reason;
    }

    public Customer saveCustomer(Long id, String name, String phone, String email, String note) {
        var c = id == null ? new Customer() : get(Customer.class, id);
        c.name = name;
        c.phone = phone;
        c.email = email;
        c.note = note;
        if (id == null)
            em.persist(c);
        return c;
    }

    public void deleteCustomer(Long id) {
        lock();
        if (em.createQuery("select count(o) from Model$Order o where customerId=:id and status='OPEN'", Long.class)
                .setParameter("id", id).getSingleResult() > 0)
            throw ApiErrors.bad("Khách hàng đang có đơn mở");
        em.remove(get(Customer.class, id));
    }

    @Transactional(readOnly = true)
    public List<Order> invoices(LocalDate from, LocalDate to) {
        return paid().stream().filter(o -> inRange(o, from, to))
                .sorted(Comparator.comparing((Order o) -> o.paidAt).reversed()).toList();
    }

    private List<Order> paid() {
        return em.createQuery("from Model$Order where status='PAID'", Order.class).getResultList();
    }

    private boolean inRange(Order o, LocalDate from, LocalDate to) {
        var date = o.paidAt.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    private BigDecimal sum(List<Order> list) {
        return list.stream().map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record DayView(BusinessDay current, long openOrders, BigDecimal sales, BigDecimal cashSales,
            BigDecimal expectedCash, List<BusinessDay> history) {
    }

    @Transactional(readOnly = true)
    public DayView day() {
        var state = get(RegisterState.class, 1L);
        var d = get(BusinessDay.class, state.dayId);
        var orders = paid().stream().filter(o -> o.dayId.equals(d.id)).toList();
        var cash = sum(orders.stream().filter(o -> "CASH".equals(o.paymentMethod)).toList());
        return new DayView(d,
                em.createQuery("select count(o) from Model$Order o where status='OPEN'", Long.class).getSingleResult(),
                sum(orders), cash, d.openingCash.add(cash), all(BusinessDay.class, "Model$BusinessDay").stream()
                        .sorted(Comparator.comparing((BusinessDay x) -> x.id).reversed()).toList());
    }

    public DayView close(BigDecimal counted, String note, String username) {
        var d = activeDay();
        var summary = day();
        if (summary.openOrders() > 0)
            throw ApiErrors.bad("Còn " + summary.openOrders() + " bàn chưa thanh toán. Hãy xử lý trước khi tất toán.");
        d.closedAt = Instant.now();
        d.countedCash = counted;
        d.expectedCash = summary.expectedCash();
        d.note = note;
        d.closedBy = username;
        return day();
    }

    public DayView start(BigDecimal cash) {
        var state = lock();
        if (get(BusinessDay.class, state.dayId).closedAt == null)
            throw ApiErrors.bad("Ngày kinh doanh hiện tại chưa đóng");
        var d = new BusinessDay();
        d.openingCash = cash;
        em.persist(d);
        state.dayId = d.id;
        return day();
    }

    public record Ranking(String name, long quantity, BigDecimal revenue) {
    }

    public record Report(BigDecimal revenue, long invoiceCount, BigDecimal average, Map<String, BigDecimal> payments,
            List<Ranking> products, List<Ranking> customers, Map<String, BigDecimal> daily) {
    }

    @Transactional(readOnly = true)
    public Report report(LocalDate from, LocalDate to) {
        var list = invoices(from, to);
        var payments = new TreeMap<String, BigDecimal>();
        var products = new HashMap<String, Ranking>();
        var customers = new HashMap<String, Ranking>();
        var daily = new TreeMap<String, BigDecimal>();
        for (var o : list) {
            payments.merge(o.paymentMethod, o.getTotal(), BigDecimal::add);
            daily.merge(o.paidAt.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate().toString(), o.getTotal(),
                    BigDecimal::add);
            for (var l : o.items) {
                var prev = products.getOrDefault(l.name, new Ranking(l.name, 0, BigDecimal.ZERO));
                products.put(l.name, new Ranking(l.name, prev.quantity() + l.quantity,
                        prev.revenue().add(l.price.multiply(BigDecimal.valueOf(l.quantity)))));
            }
            String name = o.customerName == null ? "Khách lẻ" : o.customerName;
            var prev = customers.getOrDefault(name, new Ranking(name, 0, BigDecimal.ZERO));
            customers.put(name, new Ranking(name, prev.quantity() + 1, prev.revenue().add(o.getTotal())));
        }
        var revenue = sum(list);
        return new Report(revenue, list.size(),
                list.isEmpty() ? BigDecimal.ZERO
                        : revenue.divide(BigDecimal.valueOf(list.size()), 0, RoundingMode.HALF_UP),
                payments,
                products.values().stream().sorted(Comparator.comparingLong(Ranking::quantity).reversed()).toList(),
                customers.values().stream().sorted(Comparator.comparing(Ranking::revenue).reversed()).toList(), daily);
    }
}
