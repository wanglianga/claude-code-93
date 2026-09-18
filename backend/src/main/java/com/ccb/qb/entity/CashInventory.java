package com.ccb.qb.entity;

import com.ccb.qb.model.Enums;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/** 现金库存（按面额汇总的简化模型，按网点单库 + 日期） */
@Data
@Entity
@Table(name = "cash_inventory")
public class CashInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate recordDate;

    /** 现金总余额（元） */
    @Column(nullable = false)
    private Long balance;

    /** 预警阈值 */
    @Column(nullable = false)
    private Long threshold;

    /** 大额现金业务是否需要调拨 */
    private boolean replenishing;

    @Column(length = 255)
    private String note;
}
