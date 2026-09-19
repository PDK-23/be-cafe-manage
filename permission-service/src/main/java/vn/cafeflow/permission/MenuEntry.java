package vn.cafeflow.permission;

import jakarta.persistence.*;

@Entity
public class MenuEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String label;
    public String path;
    public String icon;
    public String roles;
    public Long parentId;
    public int sortOrder;
}
