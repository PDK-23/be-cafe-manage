package vn.cafeflow.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface Accounts extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
}
