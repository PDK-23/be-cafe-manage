package vn.cafeflow.pos;

import static vn.cafeflow.pos.Model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.cafeflow.common.ApiErrors;

@RestController
@RequestMapping("/api/pos")
public class PosController {
    private final PosService pos;
    private final RestClient catalog;

    public PosController(PosService p, @Value("${cafe.catalog.url}") String url) {
        pos = p;
        catalog = RestClient.builder().baseUrl(url).build();
    }

    @GetMapping("/tables")
    public List<PosService.TableView> tables() {
        return pos.tables();
    }

    public record Add(@NotNull Long productId) {
    }

    public record CatalogItem(Long id, String name, BigDecimal price, boolean available) {
    }

    @PostMapping("/tables/{id}/items")
    public Order add(@PathVariable Long id, @Valid @RequestBody Add input, @AuthenticationPrincipal Jwt jwt) {
        CatalogItem item;
        try {
            item = catalog.get().uri("/api/catalog/products/" + input.productId())
                    .header("Authorization", "Bearer " + jwt.getTokenValue()).retrieve().body(CatalogItem.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Không thể lấy món từ thực đơn. Vui lòng thử lại.");
        }
        if (item == null || !item.available())
            throw ApiErrors.bad("Món hiện ngừng bán");
        return pos.add(id, item.id(), item.name(), item.price());
    }

    public record LineInput(@Min(0) @Max(999) int quantity, @NotNull @Size(max = 250) String note) {
    }

    @PutMapping("/orders/{id}/items/{productId}")
    public Order line(@PathVariable Long id, @PathVariable Long productId, @Valid @RequestBody LineInput i) {
        return pos.line(id, productId, i.quantity(), i.note());
    }

    public record Adjust(Long customerId,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal discount,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal surcharge,
            @NotNull @Size(max = 250) String note) {
    }

    @PutMapping("/orders/{id}")
    public Order adjust(@PathVariable Long id, @Valid @RequestBody Adjust i) {
        return pos.adjust(id, i.customerId(), i.discount(), i.surcharge(), i.note());
    }

    public record Move(@NotNull Long targetTableId) {
    }

    @PostMapping("/orders/{id}/move")
    public Order move(@PathVariable Long id, @Valid @RequestBody Move i) {
        return pos.move(id, i.targetTableId());
    }

    public record Payment(@NotNull @Min(0) Long version, @NotNull @Pattern(regexp = "CASH|TRANSFER|CARD") String method,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal tendered) {
    }

    @PostMapping("/orders/{id}/checkout")
    public Order checkout(@PathVariable Long id, @Valid @RequestBody Payment i, @AuthenticationPrincipal Jwt jwt) {
        return pos.checkoutVersioned(id, i.version(), i.method(), i.tendered(), jwt.getSubject());
    }

    public record Cancel(@NotBlank @Size(max = 250) String reason) {
    }

    @PostMapping("/orders/{id}/cancel")
    public void cancel(@PathVariable Long id, @Valid @RequestBody Cancel i) {
        pos.cancel(id, i.reason());
    }

    @GetMapping("/customers")
    public List<Customer> customers() {
        return pos.all(Customer.class, "Model$Customer");
    }

    public record CustomerInput(@NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "[0-9+]{9,15}") String phone, @Email @Size(max = 120) String email,
            @Size(max = 250) String note) {
    }

    @PostMapping("/customers")
    public Customer createCustomer(@Valid @RequestBody CustomerInput i) {
        return pos.saveCustomer(null, i.name(), i.phone(), i.email(), i.note());
    }

    @PutMapping("/customers/{id}")
    public Customer updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerInput i) {
        return pos.saveCustomer(id, i.name(), i.phone(), i.email(), i.note());
    }

    @DeleteMapping("/customers/{id}")
    public void deleteCustomer(@PathVariable Long id) {
        pos.deleteCustomer(id);
    }

    @GetMapping("/invoices")
    public List<Order> invoices(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return pos.invoices(from, to);
    }

    @GetMapping("/invoices/{id}")
    public Order invoice(@PathVariable Long id) {
        var o = pos.get(Order.class, id);
        if (!o.status.equals("PAID"))
            throw ApiErrors.missing();
        return o;
    }

    @GetMapping("/day")
    public PosService.DayView day() {
        return pos.day();
    }

    public record Close(@NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal countedCash,
            @Size(max = 250) String note) {
    }

    @PostMapping("/day/close")
    public PosService.DayView close(@Valid @RequestBody Close i, @AuthenticationPrincipal Jwt jwt) {
        return pos.close(i.countedCash(), i.note(), jwt.getSubject());
    }

    public record Start(@NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal openingCash) {
    }

    @PostMapping("/day/open")
    public PosService.DayView start(@Valid @RequestBody Start i) {
        return pos.start(i.openingCash());
    }

    @GetMapping("/reports")
    public PosService.Report report(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return pos.report(from, to);
    }
}
