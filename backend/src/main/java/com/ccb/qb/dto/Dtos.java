package com.ccb.qb.dto;

import com.ccb.qb.model.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** 请求/响应 DTO 集合（record 不可变对象） */
public final class Dtos {
    private Dtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record LoginResponse(String token, Long id, String username, String displayName,
                                String role, String skill, Long counterId) {}

    /** 客户取号/预约 */
    public record TicketRequest(
            Long customerId,
            String customerName,
            @NotNull Enums.CustomerType customerType,
            @NotNull Enums.Source source,
            @NotNull Enums.BusinessType businessType,
            @NotNull Enums.RiskLevel riskLevel,
            boolean elderly,
            boolean wheelchair,
            boolean hearingAssist,
            Integer estimatedMinutes,
            LocalDateTime appointmentTime,
            Long amount,
            /** 适老安排 */
            boolean escort,
            boolean priorityWindow,
            boolean preReview,
            boolean familyConfirm,
            String familyContact
    ) {}

    /** 大堂经理分流 */
    public record AssignRequest(
            @NotNull Enums.Channel channel,
            Long counterId,
            Long assigneeId,
            Enums.Material material,
            String materialNote,
            boolean escort,
            boolean priorityWindow,
            boolean preReview,
            boolean familyConfirm
    ) {}

    /** 协同事件上报 */
    public record EventRequest(
            Long ticketId,
            @NotNull Enums.IssueType type,
            @NotBlank String content,
            Long counterId
    ) {}

    public record EventResolveRequest(@NotBlank String resolution, boolean resumeTicket) {}

    /** 反诈核验 */
    public record FraudRequest(@NotNull Enums.FraudStatus fraudStatus, String fraudNote) {}

    /** 办理结束 */
    public record CompleteRequest(
            @NotNull Enums.ResultType resultType,
            String resultNote,
            String riskNotice,
            boolean complaint,
            String complaintNote
    ) {}

    public record CallbackRequest(String callbackNote) {}

    public record CounterStatusRequest(@NotNull Enums.CounterStatus status, String note) {}

    public record CashUpdateRequest(Long balance, Long threshold, Boolean replenishing, String note) {}

    public record AlertRequest(@NotBlank String title, String content, @NotBlank String alertType) {}
}
