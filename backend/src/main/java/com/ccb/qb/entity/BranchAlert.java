package com.ccb.qb.entity;

import com.ccb.qb.model.Enums;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 网点级特殊日/状态：养老金集中发放日、突发停电等 */
@Data
@Entity
@Table(name = "branch_alerts")
public class BranchAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String title;

    @Column(length = 400)
    private String content;

    /** PENSION_DAY / OUTAGE / NORMAL */
    @Column(nullable = false, length = 24)
    private String alertType;

    private LocalDate bizDate;

    private boolean active;

    private LocalDateTime createdAt;
}
