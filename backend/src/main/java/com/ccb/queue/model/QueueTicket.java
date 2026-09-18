package com.ccb.queue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 排队主记录：一名客户在网点的一次业务全流程。
 * 大堂经理、柜员、理财经理、安保、客服、客户围绕同一条记录协同。
 */
@Entity
@Table(name = "queue_ticket", indexes = {
        @Index(name = "idx_ticket_status", columnList = "status"),
        @Index(name = "idx_ticket_biz", columnList = "businessType")
})
public class QueueTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 叫号单号码，如 A001 / L002 / C003；先持久化取 ID 后回写，故库列允许瞬时为空 */
    @Column(unique = true, length = 10)
    private String ticketNo;

    @Column(nullable = false)
    private String customerName;

    /** 脱敏/虚拟手机号后四位 */
    private String phoneTail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Enums.CustomerType customerType = Enums.CustomerType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.TicketSource source = Enums.TicketSource.WALK_IN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.BusinessType businessType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Enums.RiskLevel riskLevel = Enums.RiskLevel.LOW;

    private boolean elder;
    private boolean wheelchair;
    private boolean hearingGuide;

    /** 预计办理时长（分钟） */
    private int estimatedMinutes = 15;

    /** 材料完整度 0-100，大堂经理初审打分 */
    private int materialScore = 100;

    /** 大额业务金额（万元），触发反诈/现金库存判断 */
    private java.math.BigDecimal amountWan = java.math.BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.TicketStatus status = Enums.TicketStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.Channel channel = Enums.Channel.UNDECIDED;

    /** 分流到的窗口号 */
    private String assignedWindow;

    private LocalDateTime createdAt;
    private LocalDateTime routedAt;
    private LocalDateTime calledAt;
    private LocalDateTime servingAt;
    private LocalDateTime finishedAt;

    /** 优先级分值，越大越优先（敬老/VIP/预约加权） */
    private int priority = 0;

    /** 反诈核验是否已通过（大额转账必须通过才可办结） */
    private boolean fraudChecked = false;
    private String fraudResult;

    /** 家属远程确认（适老场景） */
    private boolean familyConfirmed = false;
    private String familyNote;

    /** 材料预审标记 */
    private boolean materialPreChecked = false;

    /** 办理结果说明 */
    private String outcome;

    /** 客户备注/投诉内容 */
    private String complaint;

    /** 停电期间挂起 */
    private boolean suspended = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhoneTail() { return phoneTail; }
    public void setPhoneTail(String phoneTail) { this.phoneTail = phoneTail; }
    public Enums.CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(Enums.CustomerType customerType) { this.customerType = customerType; }
    public Enums.TicketSource getSource() { return source; }
    public void setSource(Enums.TicketSource source) { this.source = source; }
    public Enums.BusinessType getBusinessType() { return businessType; }
    public void setBusinessType(Enums.BusinessType businessType) { this.businessType = businessType; }
    public Enums.RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Enums.RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public boolean isElder() { return elder; }
    public void setElder(boolean elder) { this.elder = elder; }
    public boolean isWheelchair() { return wheelchair; }
    public void setWheelchair(boolean wheelchair) { this.wheelchair = wheelchair; }
    public boolean isHearingGuide() { return hearingGuide; }
    public void setHearingGuide(boolean hearingGuide) { this.hearingGuide = hearingGuide; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public int getMaterialScore() { return materialScore; }
    public void setMaterialScore(int materialScore) { this.materialScore = materialScore; }
    public java.math.BigDecimal getAmountWan() { return amountWan; }
    public void setAmountWan(java.math.BigDecimal amountWan) { this.amountWan = amountWan; }
    public Enums.TicketStatus getStatus() { return status; }
    public void setStatus(Enums.TicketStatus status) { this.status = status; }
    public Enums.Channel getChannel() { return channel; }
    public void setChannel(Enums.Channel channel) { this.channel = channel; }
    public String getAssignedWindow() { return assignedWindow; }
    public void setAssignedWindow(String assignedWindow) { this.assignedWindow = assignedWindow; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getRoutedAt() { return routedAt; }
    public void setRoutedAt(LocalDateTime routedAt) { this.routedAt = routedAt; }
    public LocalDateTime getCalledAt() { return calledAt; }
    public void setCalledAt(LocalDateTime calledAt) { this.calledAt = calledAt; }
    public LocalDateTime getServingAt() { return servingAt; }
    public void setServingAt(LocalDateTime servingAt) { this.servingAt = servingAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public boolean isFraudChecked() { return fraudChecked; }
    public void setFraudChecked(boolean fraudChecked) { this.fraudChecked = fraudChecked; }
    public String getFraudResult() { return fraudResult; }
    public void setFraudResult(String fraudResult) { this.fraudResult = fraudResult; }
    public boolean isFamilyConfirmed() { return familyConfirmed; }
    public void setFamilyConfirmed(boolean familyConfirmed) { this.familyConfirmed = familyConfirmed; }
    public String getFamilyNote() { return familyNote; }
    public void setFamilyNote(String familyNote) { this.familyNote = familyNote; }
    public boolean isMaterialPreChecked() { return materialPreChecked; }
    public void setMaterialPreChecked(boolean materialPreChecked) { this.materialPreChecked = materialPreChecked; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getComplaint() { return complaint; }
    public void setComplaint(String complaint) { this.complaint = complaint; }
    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }
}
