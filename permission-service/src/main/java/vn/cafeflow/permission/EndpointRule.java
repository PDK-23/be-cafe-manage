package vn.cafeflow.permission;

import jakarta.persistence.*;

@Entity
public class EndpointRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String role;
    public String method;
    public String path;
}
