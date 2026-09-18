package com.ccb.queue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 网点运营档案：每笔办结业务沉淀一条档案，
 * 含等待时长、办理时长、业务结果、风险提示、投诉、回访结论、渠道引导是否正确。
 */
@Entity
@Table(name = "operation_archive")
public class OperationArchive {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;
    private String ticketNo;
    private String customerName;
    private boolean elder;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Enums.CustomerType customerType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Enums.BusinessType businessType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Enums.Channel channel;

    private int waitMinutes;
    private int serveMinutes;
    private String outcome;
    private String riskTip;

    private boolean complaintRaised;
    private String complaintDetail;

    /** 回访状态与结论 */
    private boolean revisitDone;
    private String revisitResult;
    private int satisfaction;

    /** 是否被引导到正确渠道（避免业务全部挤压柜台） */
    private boolean channelCorrect = true;

    private LocalDateTime archivedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public boolean isElder() { return elder; }
    public void setElder(boolean elder) { this.elder = elder; }
    public Enums.CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(Enums.CustomerType customerType) { this.customerType = customerType; }
    public Enums.BusinessType getBusinessType() { return businessType; }
    public void setBusinessType(Enums.BusinessType businessType) { this.businessType = businessType; }
    public Enums.Channel getChannel() { return channel; }
    public void setChannel(Enums.Channel channel) { this.channel = channel; }
    public int getWaitMinutes() { return waitMinutes; }
    public void setWaitMinutes(int waitMinutes) { this.waitMinutes = waitMinutes; }
    public int getServeMinutes() { return serveMinutes; }
    public void setServeMinutes(int serveMinutes) { this.serveMinutes = serveMinutes; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getRiskTip() { return riskTip; }
    public void setRiskTip(String riskTip) { this.riskTip = riskTip; }
    public boolean isComplaintRaised() { return complaintRaised; }
    public void setComplaintRaised(boolean complaintRaised) { this.complaintRaised = complaintRaised; }
    public String getComplaintDetail() { return complaintDetail; }
    public void setComplaintDetail(String complaintDetail) { this.complaintDetail = complaintDetail; }
    public boolean isRevisitDone() { return revisitDone; }
    public void setRevisitDone(boolean revisitDone) { this.revisitDone = revisitDone; }
    public String getRevisitResult() { return revisitResult; }
    public void setRevisitResult(String revisitResult) { this.revisitResult = revisitResult; }
    public int getSatisfaction() { return satisfaction; }
    public void setSatisfaction(int satisfaction) { this.satisfaction = satisfaction; }
    public boolean isChannelCorrect() { return channelCorrect; }
    public void setChannelCorrect(boolean channelCorrect) { this.channelCorrect = channelCorrect; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
}
