package com.ccb.queue.model;

import jakarta.persistence.*;

/** 服务窗口：普通柜台、敬老优先窗、理财室、自助机 */
@Entity
@Table(name = "service_window")
public class ServiceWindow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String windowNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.WindowType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.WindowStatus status = Enums.WindowStatus.OPEN;

    /** 柜员可办理业务（枚举名逗号分隔），用于技能匹配分流 */
    @Column(length = 400)
    private String skills = "";

    /** 当前在岗员工姓名 */
    private String staffName;

    /** 当前窗口队列长度（缓存值，由分流/叫号动作维护） */
    private int currentLoad = 0;

    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWindowNo() { return windowNo; }
    public void setWindowNo(String windowNo) { this.windowNo = windowNo; }
    public Enums.WindowType getType() { return type; }
    public void setType(Enums.WindowType type) { this.type = type; }
    public Enums.WindowStatus getStatus() { return status; }
    public void setStatus(Enums.WindowStatus status) { this.status = status; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public int getCurrentLoad() { return currentLoad; }
    public void setCurrentLoad(int currentLoad) { this.currentLoad = currentLoad; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
