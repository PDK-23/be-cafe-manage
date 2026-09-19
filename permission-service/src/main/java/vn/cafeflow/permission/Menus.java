package vn.cafeflow.permission;

import org.springframework.data.jpa.repository.JpaRepository;

public interface Menus extends JpaRepository<MenuEntry, Long> {
}
