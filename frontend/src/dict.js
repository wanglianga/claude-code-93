// 与后端 Enums 保持一致的中文标签字典
export const LABELS = {
  roles: {
    MANAGER: '大堂经理', TELLER: '柜员', WEALTH_MANAGER: '理财经理',
    SECURITY: '安保', CUSTOMER_SERVICE: '客服', VIEWER: '只读观察员'
  },
  customerTypes: { NORMAL: '普通客户', VIP: 'VIP客户', ELDER: '老年客户', PENSION: '养老金客户' },
  riskLevels: { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' },
  sources: { WALK_IN: '临柜取号', APP: '手机银行', PHONE: '电话银行', ELDER_RESERVATION: '老人预约通道' },
  businessTypes: {
    OPEN_ACCOUNT: '开户', TRANSFER: '转账', REPORT_LOSS: '挂失',
    SOCIAL_CARD: '社保卡', WEALTH_CONSULT: '理财咨询', LARGE_CASH: '大额现金'
  },
  channels: {
    UNDECIDED: '待分流', COUNTER: '柜台窗口', SELF_MACHINE: '自助机具',
    WEALTH_ROOM: '理财室', REMOTE_CS: '远程客服', PRIORITY_WINDOW: '敬老优先窗'
  },
  windowTypes: { TELLER: '普通柜台', PRIORITY: '敬老优先窗', WEALTH: '理财室', SELF_SERVICE: '自助机具' },
  windowStatuses: { OPEN: '开放', CLOSED: '关闭', BREAK: '临时离岗', FAULT: '故障' },
  eventTypes: {
    LONG_WAIT: '长时间等待', FRAUD_CHECK: '大额转账反诈核验', MISSING_ID: '老人忘带证件',
    TELLER_AWAY: '柜员临时离岗', MACHINE_FAULT: '自助机故障', JUMP_QUEUE: '投诉插队',
    POWER_OUTAGE: '网点停电', LARGE_CASH: '大额现金库存', PENSION_RUSH: '养老金集中发放',
    ACCESSIBILITY: '无障碍通道', GENERAL: '其他事项'
  },
  eventStatuses: { OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已处置' },
  ticketStatuses: {
    WAITING: '候场中', CALLED: '已叫号', SERVING: '办理中',
    COMPLETED: '已办结', CANCELLED: '已取消/拦截', NOSHOW: '过号'
  },
  reservationStatuses: {
    PENDING: '待安排', ARRANGED: '已安排', ARRIVED: '已到店', DONE: '已完成', CANCELLED: '已取消'
  }
}

export function l(group, value) {
  return LABELS[group]?.[value] || value || '-'
}

export const RISK_TAG = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }
export const STATUS_TAG = {
  WAITING: 'info', CALLED: 'warning', SERVING: 'primary',
  COMPLETED: 'success', CANCELLED: 'danger', NOSHOW: 'info'
}
