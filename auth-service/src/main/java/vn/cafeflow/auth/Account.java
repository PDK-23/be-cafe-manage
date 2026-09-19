package vn.cafeflow.auth;

import jakarta.persistence.*;

@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(unique = true, nullable = false, length = 60)
    public String username;
    @Column(nullable = false)
    public String passwordHash;
    public String name;
    public String role;
    public boolean enabled = true;
    public long tokenVersion = 0;
}
