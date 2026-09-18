package com.ccb.queue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 协同事件：长等待、反诈核验、忘带证件、柜员离岗、自助机故障、
 * 投诉插队、停电、大额现金、养老金高峰、无障碍等，
 * 把大堂经理/柜员/理财经理/安保/客服/客户挂在同一排队记录上处理。
 */
@Entity
@Table(name = "collab_event")
public class CollaborationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.EventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.EventStatus status = Enums.EventStatus.OPEN;

    /** 关联排队号，网点级事件（停电/养老金高峰）可为空 */
    private Long ticketId;
    private String ticketNo;

    /** 关联窗口号（柜员离岗/自助机故障） */
    private String windowNo;

    @Column(nullable = false, length = 500)
    private String description;

    /** 当前责任角色 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Enums.Role ownerRole;

    private String raisedBy;
    private LocalDateTime raisedAt;
    private LocalDateTime resolvedAt;
    private String resolution;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Enums.EventType getType() { return type; }
    public void setType(Enums.EventType type) { this.type = type; }
    public Enums.EventStatus getStatus() { return status; }
    public void setStatus(Enums.EventStatus status) { this.status = status; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getWindowNo() { return windowNo; }
    public void setWindowNo(String windowNo) { this.windowNo = windowNo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Enums.Role getOwnerRole() { return ownerRole; }
    public void setOwnerRole(Enums.Role ownerRole) { this.ownerRole = ownerRole; }
    public String getRaisedBy() { return raisedBy; }
    public void setRaisedBy(String raisedBy) { this.raisedBy = raisedBy; }
    public LocalDateTime getRaisedAt() { return raisedAt; }
    public void setRaisedAt(LocalDateTime raisedAt) { this.raisedAt = raisedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
}
