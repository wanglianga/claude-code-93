package com.ccb.queue.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.Role role;

    /** 柜员/理财经理绑定的窗口编号（可空） */
    private String windowNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Enums.Role getRole() { return role; }
    public void setRole(Enums.Role role) { this.role = role; }
    public String getWindowNo() { return windowNo; }
    public void setWindowNo(String windowNo) { this.windowNo = windowNo; }
}
