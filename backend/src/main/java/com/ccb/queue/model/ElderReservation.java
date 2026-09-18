package com.ccb.queue.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 老人/适老服务预约：陪同引导、优先窗口、材料预审、家属远程确认 */
@Entity
@Table(name = "elder_reservation")
public class ElderReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String elderName;

    private String phoneTail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.BusinessType businessType;

    private LocalDateTime reserveTime;

    private boolean wheelchair;
    private boolean hearingGuide;
    private boolean escortNeeded = true;
    private boolean priorityWindow = true;
    private boolean materialPreReview = true;

    /** 家属联系方式（脱敏）与远程确认状态 */
    private String familyContact;
    private boolean familyRemoteConfirmed = false;
    private String familyNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.ReservationStatus status = Enums.ReservationStatus.PENDING;

    /** 安排的陪同人（大堂/安保/志愿者） */
    private String escortStaff;

    /** 到店后生成的排队号 */
    private String ticketNo;

    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getElderName() { return elderName; }
    public void setElderName(String elderName) { this.elderName = elderName; }
    public String getPhoneTail() { return phoneTail; }
    public void setPhoneTail(String phoneTail) { this.phoneTail = phoneTail; }
    public Enums.BusinessType getBusinessType() { return businessType; }
    public void setBusinessType(Enums.BusinessType businessType) { this.businessType = businessType; }
    public LocalDateTime getReserveTime() { return reserveTime; }
    public void setReserveTime(LocalDateTime reserveTime) { this.reserveTime = reserveTime; }
    public boolean isWheelchair() { return wheelchair; }
    public void setWheelchair(boolean wheelchair) { this.wheelchair = wheelchair; }
    public boolean isHearingGuide() { return hearingGuide; }
    public void setHearingGuide(boolean hearingGuide) { this.hearingGuide = hearingGuide; }
    public boolean isEscortNeeded() { return escortNeeded; }
    public void setEscortNeeded(boolean escortNeeded) { this.escortNeeded = escortNeeded; }
    public boolean isPriorityWindow() { return priorityWindow; }
    public void setPriorityWindow(boolean priorityWindow) { this.priorityWindow = priorityWindow; }
    public boolean isMaterialPreReview() { return materialPreReview; }
    public void setMaterialPreReview(boolean materialPreReview) { this.materialPreReview = materialPreReview; }
    public String getFamilyContact() { return familyContact; }
    public void setFamilyContact(String familyContact) { this.familyContact = familyContact; }
    public boolean isFamilyRemoteConfirmed() { return familyRemoteConfirmed; }
    public void setFamilyRemoteConfirmed(boolean familyRemoteConfirmed) { this.familyRemoteConfirmed = familyRemoteConfirmed; }
    public String getFamilyNote() { return familyNote; }
    public void setFamilyNote(String familyNote) { this.familyNote = familyNote; }
    public Enums.ReservationStatus getStatus() { return status; }
    public void setStatus(Enums.ReservationStatus status) { this.status = status; }
    public String getEscortStaff() { return escortStaff; }
    public void setEscortStaff(String escortStaff) { this.escortStaff = escortStaff; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
