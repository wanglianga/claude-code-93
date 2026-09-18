package com.ccb.qb.entity;

import com.ccb.qb.model.Enums;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排队协同事件：长等/反诈核验/忘带证件/柜员离岗/自助机故障/投诉插队/突发停电。
 * 每一条事件挂在排队记录上，记录六方中的哪一方在何时做了什么。
 */
@Data
@Entity
@Table(name = "queue_events")
public class QueueEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.IssueType type;

    @Column(nullable = false, length = 400)
    private String content;

    @Column(length = 32)
    private String actorName;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Enums.Role actorRole;

    /** 关联窗口（柜员离岗/自助机故障/停电时） */
    private Long counterId;

    /** 是否已解决 */
    private boolean resolved;

    @Column(length = 400)
    private String resolution;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
