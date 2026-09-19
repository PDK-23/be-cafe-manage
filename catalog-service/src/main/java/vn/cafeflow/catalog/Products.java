package vn.cafeflow.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface Products extends JpaRepository<Product, Long> {
    boolean existsByCategoryId(Long id);
}
