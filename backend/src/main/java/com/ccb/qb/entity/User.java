package com.ccb.qb.entity;

import com.ccb.qb.model.Enums;
import jakarta.persistence.*;
import lombok.Data;

/** 系统用户：客户/大堂经理/柜员/理财经理/安保/客服 */
@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 32)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Enums.Role role;

    /** 岗位描述/专长，如“现金业务、挂失” */
    @Column(length = 128)
    private String skill;

    /** 柜员/理财经理所属窗口（可空） */
    private Long counterId;

    private boolean enabled = true;
}
