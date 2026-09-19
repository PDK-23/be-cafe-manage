package vn.cafeflow.catalog;

import java.util.*;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import vn.cafeflow.common.ApiErrors;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final Categories categories;
    private final Products products;

    public CatalogController(Categories c, Products p) {
        categories = c;
        products = p;
    }

    public record CategoryInput(@NotBlank @Size(max = 80) String name) {
    }

    public record ProductInput(@NotBlank @Size(max = 120) String name, @NotNull Long categoryId,
            @NotNull @DecimalMin("0") @DecimalMax("100000000") @Digits(integer = 9, fraction = 0) BigDecimal price,
            boolean available, @Size(max = 30) String icon) {
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return categories.findAll();
    }

    @PostMapping("/categories")
    public Category addCategory(@Valid @RequestBody CategoryInput i) {
        var c = new Category();
        c.name = i.name();
        return categories.save(c);
    }

    @PutMapping("/categories/{id}")
    public Category editCategory(@PathVariable Long id, @Valid @RequestBody CategoryInput i) {
        var c = categories.findById(id).orElseThrow(ApiErrors::missing);
        c.name = i.name();
        return categories.save(c);
    }

    @DeleteMapping("/categories/{id}")
    @Transactional
    public void deleteCategory(@PathVariable Long id) {
        if (products.existsByCategoryId(id))
            throw ApiErrors.bad("Danh mục còn món, hãy chuyển hoặc xóa món trước");
        categories.deleteById(id);
    }

    @GetMapping("/products")
    public List<Product> products() {
        return products.findAll();
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable Long id) {
        return products.findById(id).orElseThrow(ApiErrors::missing);
    }

    @PostMapping("/products")
    public Product create(@Valid @RequestBody ProductInput i) {
        return save(new Product(), i);
    }

    @PutMapping("/products/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductInput i) {
        return save(product(id), i);
    }

    @DeleteMapping("/products/{id}")
    public void delete(@PathVariable Long id) {
        products.deleteById(id);
    }

    private Product save(Product p, ProductInput i) {
        if (!categories.existsById(i.categoryId()))
            throw ApiErrors.bad("Danh mục không tồn tại");
        p.name = i.name();
        p.categoryId = i.categoryId();
        p.price = i.price();
        p.available = i.available();
        p.icon = i.icon();
        return products.save(p);
    }
}
