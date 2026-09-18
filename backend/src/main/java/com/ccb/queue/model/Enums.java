package com.ccb.queue.model;

/** 平台全部枚举集中定义，便于业务字典统一维护 */
public final class Enums {
    private Enums() {}

    /** 系统角色：大堂经理 / 柜员 / 理财经理 / 安保 / 客服 / 只读演示 */
    public enum Role { MANAGER, TELLER, WEALTH_MANAGER, SECURITY, CUSTOMER_SERVICE, VIEWER }

    public enum CustomerType { NORMAL, VIP, ELDER, PENSION }

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    /** 预约/取号来源：临柜、手机银行、电话银行、老人预约通道 */
    public enum TicketSource { WALK_IN, APP, PHONE, ELDER_RESERVATION }

    /** 业务类型：开户 / 转账 / 挂失 / 社保卡 / 理财咨询 / 大额现金 */
    public enum BusinessType {
        OPEN_ACCOUNT("开户"), TRANSFER("转账"), REPORT_LOSS("挂失"),
        SOCIAL_CARD("社保卡"), WEALTH_CONSULT("理财咨询"), LARGE_CASH("大额现金");
        public final String label;
        BusinessType(String label) { this.label = label; }
    }

    public enum TicketStatus {
        WAITING("候场中"), CALLED("已叫号"), SERVING("办理中"),
        COMPLETED("已办结"), CANCELLED("已取消/拦截"), NOSHOW("过号");
        public final String label;
        TicketStatus(String label) { this.label = label; }
    }

    /** 实际引导渠道：柜台 / 自助机 / 理财室 / 远程客服 */
    public enum Channel {
        UNDECIDED("待分流"), COUNTER("柜台窗口"), SELF_MACHINE("自助机具"),
        WEALTH_ROOM("理财室"), REMOTE_CS("远程客服"), PRIORITY_WINDOW("敬老优先窗");
        public final String label;
        Channel(String label) { this.label = label; }
    }

    public enum WindowType { TELLER, PRIORITY, WEALTH, SELF_SERVICE }

    public enum WindowStatus { OPEN("开放"), CLOSED("关闭"), BREAK("临时离岗"), FAULT("故障");
        public final String label;
        WindowStatus(String label) { this.label = label; }
    }

    /** 协同事件类型：长等待、反诈核验、忘带证件、柜员离岗、自助机故障、投诉插队、停电、大额现金、养老金高峰、无障碍 */
    public enum EventType {
        LONG_WAIT("长时间等待"), FRAUD_CHECK("大额转账反诈核验"), MISSING_ID("老人忘带证件"),
        TELLER_AWAY("柜员临时离岗"), MACHINE_FAULT("自助机故障"), JUMP_QUEUE("投诉插队"),
        POWER_OUTAGE("网点停电"), LARGE_CASH("大额现金库存"), PENSION_RUSH("养老金集中发放"),
        ACCESSIBILITY("无障碍通道"), GENERAL("其他事项");
        public final String label;
        EventType(String label) { this.label = label; }
    }

    public enum EventStatus { OPEN("待处理"), PROCESSING("处理中"), RESOLVED("已处置");
        public final String label;
        EventStatus(String label) { this.label = label; }
    }

    public enum ReservationStatus { PENDING("待安排"), ARRANGED("已安排"), ARRIVED("已到店"), DONE("已完成"), CANCELLED("已取消");
        public final String label;
        ReservationStatus(String label) { this.label = label; }
    }
}
