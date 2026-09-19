package vn.cafeflow.permission;

import org.springframework.data.jpa.repository.JpaRepository;

public interface Rules extends JpaRepository<EndpointRule, Long> {
}
