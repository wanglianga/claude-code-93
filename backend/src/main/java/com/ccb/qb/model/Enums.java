package com.ccb.qb.model;

/** 平台使用的全部枚举集中定义 */
public final class Enums {
    private Enums() {}

    /** 六类登录角色 + 客户 */
    public enum Role { CUSTOMER, MANAGER, TELLER, ADVISOR, SECURITY, SERVICE }

    /** 客户类型 */
    public enum CustomerType { NORMAL, VIP, ELDERLY, CORPORATE }

    /** 预约/取号来源 */
    public enum Source { WALK_IN, MOBILE_APP, PHONE, HOTLINE_ELDERLY }

    /** 业务类型：开户/转账/挂失/社保卡/理财咨询/大额现金 */
    public enum BusinessType {
        ACCOUNT_OPEN, TRANSFER, LOST_REPORT, SOCIAL_CARD, WEALTH_CONSULT, LARGE_CASH
    }

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    /** 材料完整度 */
    public enum Material { COMPLETE, INCOMPLETE, MISSING }

    /** 排队记录状态：等待分流/已分配/办理中/异常挂起/已完成/已取消 */
    public enum TicketStatus { WAITING, ASSIGNED, SERVING, PENDING, DONE, CANCELLED }

    /** 引导渠道：柜台/自助机/理财室/远程客服 */
    public enum Channel { COUNTER, SELF_MACHINE, WEALTH_ROOM, REMOTE_SERVICE }

    /** 窗口类型：综合/现金/爱心优先/自助设备 */
    public enum CounterType { GENERAL, CASH, PRIORITY, SELF_SERVICE }

    public enum CounterStatus { OPEN, TEMP_LEAVE, CLOSED, FAULT }

    /** 异常事件类型：长等/反诈/忘带证件/柜员离岗/自助机故障/投诉插队/停电 */
    public enum IssueType {
        NONE, LONG_WAIT, FRAUD_CHECK, MISSING_ID, TELLER_LEAVE,
        MACHINE_FAULT, JUMP_COMPLAINT, OUTAGE
    }

    /** 反诈核验状态 */
    public enum FraudStatus { NONE, PENDING, PASSED, BLOCKED }

    public enum ResultType { SUCCESS, REJECTED, ESCALATED, REDIRECTED }

    public enum YesNo { OPEN, CLOSED }
}
