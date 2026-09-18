package com.ccb.queue.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 网点现金库存记录：大额现金业务需校验库存，不足触发协同事件（调拨/预约） */
@Entity
@Table(name = "cash_inventory")
public class CashInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String denomination = "100元";

    /** 可用现金（万元） */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountWan = BigDecimal.ZERO;

    private LocalDateTime updatedAt;

    public CashInventory() {}
    public CashInventory(String denomination, BigDecimal amountWan) {
        this.denomination = denomination;
        this.amountWan = amountWan;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDenomination() { return denomination; }
    public void setDenomination(String denomination) { this.denomination = denomination; }
    public BigDecimal getAmountWan() { return amountWan; }
    public void setAmountWan(BigDecimal amountWan) { this.amountWan = amountWan; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
