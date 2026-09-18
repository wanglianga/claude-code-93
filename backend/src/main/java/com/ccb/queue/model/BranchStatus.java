package com.ccb.queue.model;

import jakarta.persistence.*;

/** 网点运营全局状态：正常营业 / 停电应急 / 养老金集中发放日 */
@Entity
@Table(name = "branch_status")
public class BranchStatus {
    @Id
    private Long id = 1L;

    private boolean powerOutage = false;

    /** 养老金集中发放日：老人/养老金客户流量高峰，自动加权并增开优先窗 */
    private boolean pensionDay = false;

    private String notice = "";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public boolean isPowerOutage() { return powerOutage; }
    public void setPowerOutage(boolean powerOutage) { this.powerOutage = powerOutage; }
    public boolean isPensionDay() { return pensionDay; }
    public void setPensionDay(boolean pensionDay) { this.pensionDay = pensionDay; }
    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }
}
