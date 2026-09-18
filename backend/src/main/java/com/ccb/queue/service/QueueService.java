package com.ccb.queue.service;

import com.ccb.queue.model.*;
import com.ccb.queue.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 排队分流与协同核心服务。
 * 规则均为可演示的业务策略：敬老优先、材料完整度、柜员技能、窗口负载、
 * 现金库存、反诈阈值、渠道引导（柜台/自助机/理财室/远程客服）。
 */
@Service
public class QueueService {

    /** 大额转账反诈核验阈值（万元） */
    public static final BigDecimal FRAUD_THRESHOLD = new BigDecimal("20");
    /** 长等待阈值（分钟） */
    public static final int LONG_WAIT_MINUTES = 20;

    private final TicketRepository ticketRepo;
    private final WindowRepository windowRepo;
    private final EventRepository eventRepo;
    private final ReservationRepository reservationRepo;
    private final CashInventoryRepository cashRepo;
    private final BranchStatusRepository branchRepo;
    private final ArchiveRepository archiveRepo;

    public QueueService(TicketRepository ticketRepo, WindowRepository windowRepo,
                        EventRepository eventRepo, ReservationRepository reservationRepo,
                        CashInventoryRepository cashRepo, BranchStatusRepository branchRepo,
                        ArchiveRepository archiveRepo) {
        this.ticketRepo = ticketRepo;
        this.windowRepo = windowRepo;
        this.eventRepo = eventRepo;
        this.reservationRepo = reservationRepo;
        this.cashRepo = cashRepo;
        this.branchRepo = branchRepo;
        this.archiveRepo = archiveRepo;
    }

    // ---------------- 取号 ----------------

    @Transactional
    public QueueTicket createTicket(Map<String, Object> body, String operator) {
        QueueTicket t = new QueueTicket();
        t.setCustomerName(str(body, "customerName", "匿名客户"));
        t.setPhoneTail(str(body, "phoneTail", ""));
        t.setCustomerType(Enums.CustomerType.valueOf(str(body, "customerType", "NORMAL")));
        t.setSource(Enums.TicketSource.valueOf(str(body, "source", "WALK_IN")));
        t.setBusinessType(Enums.BusinessType.valueOf(str(body, "businessType", "OPEN_ACCOUNT")));
        t.setRiskLevel(Enums.RiskLevel.valueOf(str(body, "riskLevel", "LOW")));
        t.setElder(bool(body, "elder", false) || t.getCustomerType() == Enums.CustomerType.ELDER);
        t.setWheelchair(bool(body, "wheelchair", false));
        t.setHearingGuide(bool(body, "hearingGuide", false));
        t.setEstimatedMinutes(intVal(body, "estimatedMinutes", defaultMinutes(t.getBusinessType())));
        t.setMaterialScore(Math.max(0, Math.min(100, intVal(body, "materialScore", 100))));
        BigDecimal amount = dec(body, "amountWan");
        t.setAmountWan(amount);
        t.setCreatedAt(LocalDateTime.now());
        t.setPriority(basePriority(t));

        // 风险联动：大额转账/大额现金直接标记中高风险
        if (t.getBusinessType() == Enums.BusinessType.TRANSFER && amount.compareTo(FRAUD_THRESHOLD) >= 0) {
            t.setRiskLevel(Enums.RiskLevel.HIGH);
        }

        QueueTicket saved = ticketRepo.save(t);
        saved.setTicketNo(prefix(saved.getBusinessType()) + String.format("%03d", saved.getId()));
        ticketRepo.save(saved);
        return saved;
    }

    private int defaultMinutes(Enums.BusinessType type) {
        return switch (type) {
            case OPEN_ACCOUNT -> 20;
            case TRANSFER -> 15;
            case REPORT_LOSS -> 25;
            case SOCIAL_CARD -> 20;
            case WEALTH_CONSULT -> 30;
            case LARGE_CASH -> 25;
        };
    }

    private int basePriority(QueueTicket t) {
        int p = 0;
        if (t.isElder()) p += 40;
        if (t.getCustomerType() == Enums.CustomerType.VIP) p += 30;
        if (t.getCustomerType() == Enums.CustomerType.PENSION) p += 20;
        if (t.getSource() != Enums.TicketSource.WALK_IN) p += 10;
        if (t.getRiskLevel() == Enums.RiskLevel.HIGH) p += 5;
        if (branchStatus().isPensionDay()
                && (t.isElder() || t.getCustomerType() == Enums.CustomerType.PENSION)) {
            p += 25;
        }
        return p;
    }

    private String prefix(Enums.BusinessType type) {
        return switch (type) {
            case OPEN_ACCOUNT -> "A";
            case TRANSFER -> "H";
            case REPORT_LOSS -> "G";
            case SOCIAL_CARD -> "S";
            case WEALTH_CONSULT -> "L";
            case LARGE_CASH -> "X";
        };
    }

    // ---------------- 分流 ----------------

    /** 自动分流建议：返回建议渠道、窗口与理由，大堂经理可采纳或手动改派 */
    @Transactional
    public Map<String, Object> suggestRoute(Long ticketId) {
        QueueTicket t = ticketRepo.findById(ticketId).orElseThrow();
        BranchStatus bs = branchStatus();

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> reasons = new ArrayList<>();

        if (bs.isPowerOutage()) {
            result.put("channel", Enums.Channel.UNDECIDED);
            result.put("windowNo", null);
            result.put("reasons", List.of("网点停电应急中：暂停电子叫号，启用手工登记，请安保维持秩序"));
            result.put("blocked", true);
            return result;
        }

        Enums.Channel channel;
        Enums.WindowType windowType;

        // 1) 适老/无障碍：敬老优先窗
        if (t.isElder() || t.isWheelchair()) {
            channel = Enums.Channel.PRIORITY_WINDOW;
            windowType = Enums.WindowType.PRIORITY;
            reasons.add("老人/轮椅客户，安排敬老优先窗");
            if (t.isHearingGuide()) reasons.add("已勾选听力引导，需佩戴扩音设备柜员接待");
        } else if (t.getBusinessType() == Enums.BusinessType.WEALTH_CONSULT) {
            // 2) 理财咨询 → 理财室（理财经理排班）
            channel = Enums.Channel.WEALTH_ROOM;
            windowType = Enums.WindowType.WEALTH;
            reasons.add("理财咨询引导至理财室，由当班理财经理接待");
        } else if (t.getBusinessType() == Enums.BusinessType.OPEN_ACCOUNT
                && t.getMaterialScore() >= 80 && t.getRiskLevel() != Enums.RiskLevel.HIGH) {
            // 3) 材料齐全的开户 → 智能柜员机
            channel = Enums.Channel.SELF_MACHINE;
            windowType = Enums.WindowType.SELF_SERVICE;
            reasons.add("材料完整度" + t.getMaterialScore() + "分，开户可在智能柜员机完成，大堂人员现场协助");
        } else if (t.getBusinessType() == Enums.BusinessType.TRANSFER
                && t.getAmountWan().compareTo(new BigDecimal("5")) < 0
                && t.getRiskLevel() == Enums.RiskLevel.LOW) {
            // 4) 小额低风险转账 → 自助机/远程客服
            channel = Enums.Channel.SELF_MACHINE;
            windowType = Enums.WindowType.SELF_SERVICE;
            reasons.add("5万元以下低风险转账引导自助渠道办理，避免挤占柜台");
        } else if (t.getBusinessType() == Enums.BusinessType.TRANSFER
                && t.getAmountWan().compareTo(FRAUD_THRESHOLD) >= 0) {
            channel = Enums.Channel.COUNTER;
            windowType = Enums.WindowType.TELLER;
            reasons.add("大额转账" + t.getAmountWan() + "万元，必须柜台办理并先通过反诈核验");
        } else if (t.getBusinessType() == Enums.BusinessType.LARGE_CASH) {
            channel = Enums.Channel.COUNTER;
            windowType = Enums.WindowType.TELLER;
            reasons.add("大额现金业务安排现金柜台");
            BigDecimal total = totalCash();
            if (total.compareTo(t.getAmountWan()) < 0) {
                reasons.add("预警：当前现金库存" + total + "万元，不足" + t.getAmountWan() + "万元，需调拨或预约改日");
            }
        } else {
            channel = Enums.Channel.COUNTER;
            windowType = Enums.WindowType.TELLER;
            reasons.add("业务须人工柜面办理（" + t.getBusinessType().label + "）");
        }

        if (t.getMaterialScore() < 60) {
            reasons.add("材料完整度仅" + t.getMaterialScore() + "分，建议先做材料预审/一次性告知，避免空等");
        }
        if (bs.isPensionDay()) reasons.add("今日为养老金集中发放日，厅堂客流高峰，已启用优先通道");

        ServiceWindow win = pickWindow(windowType, t.getBusinessType());
        if (win == null && windowType != Enums.WindowType.SELF_SERVICE) {
            // 优先窗/理财室满负荷时回退普通柜台
            win = pickWindow(Enums.WindowType.TELLER, t.getBusinessType());
            if (win != null) reasons.add("专属窗口无空闲，回退至负载最低的普通柜台");
        }
        result.put("channel", channel);
        result.put("windowNo", win == null ? null : win.getWindowNo());
        result.put("reasons", reasons);
        result.put("blocked", false);
        return result;
    }

    /** 执行分流（大堂经理采纳建议或手动指定渠道/窗口） */
    @Transactional
    public QueueTicket route(Long ticketId, Map<String, Object> body, String operator) {
        QueueTicket t = ticketRepo.findById(ticketId).orElseThrow();
        if (t.getStatus() != Enums.TicketStatus.WAITING) {
            throw new IllegalStateException("该号已分流/已办理，不能重复分流");
        }
        if (branchStatus().isPowerOutage()) {
            throw new IllegalStateException("网点停电应急中，暂停电子分流");
        }
        Enums.Channel channel = Enums.Channel.valueOf(str(body, "channel", "COUNTER"));
        String windowNo = str(body, "windowNo", null);

        t.setChannel(channel);
        t.setAssignedWindow(windowNo);
        t.setRoutedAt(LocalDateTime.now());
        if (bool(body, "materialPreChecked", t.isMaterialPreChecked())) {
            t.setMaterialPreChecked(true);
        }

        // 自助机/远程客服渠道：直接进入"办理中"的自助流，由大堂人员协助
        if (channel == Enums.Channel.SELF_MACHINE || channel == Enums.Channel.REMOTE_CS) {
            t.setStatus(Enums.TicketStatus.SERVING);
            t.setCalledAt(LocalDateTime.now());
            t.setServingAt(LocalDateTime.now());
        } else {
            t.setStatus(Enums.TicketStatus.WAITING);
        }

        // 大额转账分流时自动拉起反诈核验协同事件
        if (t.getBusinessType() == Enums.BusinessType.TRANSFER
                && t.getAmountWan().compareTo(FRAUD_THRESHOLD) >= 0
                && !t.isFraudChecked()) {
            raiseEvent(Enums.EventType.FRAUD_CHECK, t.getId(), t.getTicketNo(), null,
                    "客户" + t.getCustomerName() + "申请大额转账" + t.getAmountWan()
                            + "万元，需完成反诈四问与核验后方可办理",
                    Enums.Role.TELLER, operator);
        }
        // 大额现金库存预警
        if (t.getBusinessType() == Enums.BusinessType.LARGE_CASH
                && totalCash().compareTo(t.getAmountWan()) < 0) {
            raiseEvent(Enums.EventType.LARGE_CASH, t.getId(), t.getTicketNo(), windowNo,
                    "现金库存不足，客户需求" + t.getAmountWan() + "万元，当前合计" + totalCash() + "万元",
                    Enums.Role.MANAGER, operator);
        }
        ticketRepo.save(t);
        recalcAllLoads();
        return t;
    }

    private ServiceWindow pickWindow(Enums.WindowType type, Enums.BusinessType biz) {
        return windowRepo.findAll().stream()
                .filter(w -> w.getType() == type)
                .filter(w -> w.getStatus() == Enums.WindowStatus.OPEN)
                .filter(w -> w.getSkills() == null || w.getSkills().isBlank()
                        || Arrays.asList(w.getSkills().split(",")).contains(biz.name()))
                .min(Comparator.comparingInt(ServiceWindow::getCurrentLoad)
                        .thenComparing(ServiceWindow::getWindowNo))
                .orElse(null);
    }

    // ---------------- 叫号 / 办理 / 办结 ----------------

    /** 窗口呼叫下一位 */
    @Transactional
    public QueueTicket callNext(String windowNo, String operator) {
        ServiceWindow w = windowRepo.findByWindowNo(windowNo)
                .orElseThrow(() -> new IllegalArgumentException("窗口不存在: " + windowNo));
        if (w.getStatus() != Enums.WindowStatus.OPEN) {
            throw new IllegalStateException("窗口当前为「" + w.getStatus().label + "」状态，无法叫号");
        }
        QueueTicket next = ticketRepo
                .findByAssignedWindowAndStatusIn(windowNo, List.of(Enums.TicketStatus.WAITING))
                .stream()
                .max(Comparator.comparingInt(QueueTicket::getPriority)
                        .thenComparing(QueueTicket::getCreatedAt))
                .orElseThrow(() -> new IllegalStateException("该窗口暂无等候客户"));
        next.setStatus(Enums.TicketStatus.CALLED);
        next.setCalledAt(LocalDateTime.now());
        ticketRepo.save(next);
        return next;
    }

    @Transactional
    public QueueTicket startServing(Long ticketId, String operator) {
        QueueTicket t = ticketRepo.findById(ticketId).orElseThrow();
        t.setStatus(Enums.TicketStatus.SERVING);
        t.setServingAt(LocalDateTime.now());
        ticketRepo.save(t);
        recalcAllLoads();
        return t;
    }

    /** 办结：反诈/库存合规闸门通过后归档到运营档案 */
    @Transactional
    public OperationArchive finish(Long ticketId, Map<String, Object> body, String operator) {
        QueueTicket t = ticketRepo.findById(ticketId).orElseThrow();

        // 合规闸门1：大额转账必须反诈核验通过
        if (t.getBusinessType() == Enums.BusinessType.TRANSFER
                && t.getAmountWan().compareTo(FRAUD_THRESHOLD) >= 0 && !t.isFraudChecked()) {
            throw new IllegalStateException("大额转账未通过反诈核验，不能办结，请先在协同事件中完成核验");
        }
        // 合规闸门2：老人高风险业务建议有家属远程确认（预约场景强校验，临柜由经理人工放行）
        if (t.isElder() && t.getRiskLevel() == Enums.RiskLevel.HIGH
                && !t.isFamilyConfirmed() && bool(body, "requireFamilyConfirm", false)) {
            throw new IllegalStateException("老人高风险业务尚未取得家属远程确认");
        }
        // 合规闸门3：大额现金先扣减库存
        if (t.getBusinessType() == Enums.BusinessType.LARGE_CASH
                && t.getAmountWan().compareTo(BigDecimal.ZERO) > 0) {
            if (!consumeCash(t.getAmountWan())) {
                throw new IllegalStateException("现金库存不足，请先调拨现金或与客户改约");
            }
        }

        t.setStatus(Enums.TicketStatus.COMPLETED);
        t.setFinishedAt(LocalDateTime.now());
        t.setOutcome(str(body, "outcome", "业务办理成功"));
        ticketRepo.save(t);
        recalcAllLoads();
        return archive(t, t.getOutcome(), riskTip(t));
    }

    /** 反诈拦截：业务终止，同样进入运营档案留痕 */
    @Transactional
    public OperationArchive fraudIntercept(Long ticketId, String reason, String operator) {
        QueueTicket t = ticketRepo.findById(ticketId).orElseThrow();
        t.setStatus(Enums.TicketStatus.CANCELLED);
        t.setFinishedAt(LocalDateTime.now());
        t.setFraudChecked(false);
        t.setFraudResult("核验不通过：" + reason);
        t.setOutcome("反诈拦截，业务终止");
        ticketRepo.save(t);
        recalcAllLoads();
        OperationArchive a = archive(t, "反诈拦截，业务终止",
                "疑似涉诈转账" + t.getAmountWan() + "万元，已劝阻并登记：" + reason);
        // 同步关闭该单的反诈事件
        eventRepo.findByTicketIdOrderByRaisedAtAsc(ticketId).stream()
                .filter(e -> e.getType() == Enums.EventType.FRAUD_CHECK
                        && e.getStatus() != Enums.EventStatus.RESOLVED)
                .forEach(e -> resolveEvent(e.getId(),
                        "反诈核验不通过，已拦截并劝阻客户：" + reason, operator));
        return a;
    }

    /** 反诈核验通过 */
    @Transactional
    public QueueTicket fraudPass(Long ticketId, String note, String operator) {
        QueueTicket t = ticketRepo.findById(ticketId).orElseThrow();
        t.setFraudChecked(true);
        t.setFraudResult("核验通过：" + note);
        ticketRepo.save(t);
        eventRepo.findByTicketIdOrderByRaisedAtAsc(ticketId).stream()
                .filter(e -> e.getType() == Enums.EventType.FRAUD_CHECK
                        && e.getStatus() != Enums.EventStatus.RESOLVED)
                .forEach(e -> resolveEvent(e.getId(), "反诈四问通过、收款人信息核验无误：" + note, operator));
        return t;
    }

    private String riskTip(QueueTicket t) {
        List<String> tips = new ArrayList<>();
        if (t.getRiskLevel() == Enums.RiskLevel.HIGH) tips.add("高风险业务，已双人复核");
        if (t.isFraudChecked()) tips.add("已完成反诈核验");
        if (t.getBusinessType() == Enums.BusinessType.LARGE_CASH) tips.add("大额现金，已核验身份证并登记台账");
        if (t.isElder()) tips.add("老年客户，已做风险提示与适老关怀");
        return String.join("；", tips);
    }

    private OperationArchive archive(QueueTicket t, String outcome, String riskTip) {
        OperationArchive a = new OperationArchive();
        a.setTicketId(t.getId());
        a.setTicketNo(t.getTicketNo());
        a.setCustomerName(t.getCustomerName());
        a.setElder(t.isElder());
        a.setCustomerType(t.getCustomerType());
        a.setBusinessType(t.getBusinessType());
        a.setChannel(t.getChannel());
        LocalDateTime start = t.getCreatedAt();
        LocalDateTime end = t.getFinishedAt() != null ? t.getFinishedAt() : LocalDateTime.now();
        a.setWaitMinutes(minutesBetween(start, t.getServingAt() != null ? t.getServingAt() : end));
        a.setServeMinutes(t.getServingAt() == null ? 0
                : Math.max(0, minutesBetween(t.getServingAt(), end)));
        a.setOutcome(outcome);
        a.setRiskTip(riskTip);
        a.setComplaintRaised(t.getComplaint() != null && !t.getComplaint().isBlank());
        a.setComplaintDetail(t.getComplaint());
        a.setChannelCorrect(judgeChannelCorrect(t));
        a.setArchivedAt(LocalDateTime.now());
        return archiveRepo.save(a);
    }

    /** 是否被引导到正确渠道：可自助的业务不应占用柜台 */
    private boolean judgeChannelCorrect(QueueTicket t) {
        boolean counterHeavy = t.getChannel() == Enums.Channel.COUNTER
                || t.getChannel() == Enums.Channel.PRIORITY_WINDOW;
        if (!counterHeavy) return true;
        if (t.isElder() || t.isWheelchair()) return true; // 老人优先窗是正确引导
        return switch (t.getBusinessType()) {
            case OPEN_ACCOUNT -> t.getMaterialScore() < 80
                    || t.getRiskLevel() == Enums.RiskLevel.HIGH;
            case TRANSFER -> t.getAmountWan().compareTo(new BigDecimal("5")) >= 0
                    || t.getRiskLevel() != Enums.RiskLevel.LOW;
            default -> true;
        };
    }

    private int minutesBetween(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) return 0;
        long m = Duration.between(a, b).toMinutes();
        return (int) Math.max(0, m);
    }

    // ---------------- 适老预约 ----------------

    @Transactional
    public ElderReservation createReservation(Map<String, Object> body) {
        ElderReservation r = new ElderReservation();
        r.setElderName(str(body, "elderName", "老年客户"));
        r.setPhoneTail(str(body, "phoneTail", ""));
        r.setBusinessType(Enums.BusinessType.valueOf(str(body, "businessType", "SOCIAL_CARD")));
        String rt = str(body, "reserveTime", null);
        r.setReserveTime(rt == null || rt.isBlank() ? LocalDateTime.now().plusHours(1)
                : LocalDateTime.parse(rt));
        r.setWheelchair(bool(body, "wheelchair", false));
        r.setHearingGuide(bool(body, "hearingGuide", false));
        r.setEscortNeeded(bool(body, "escortNeeded", true));
        r.setPriorityWindow(bool(body, "priorityWindow", true));
        r.setMaterialPreReview(bool(body, "materialPreReview", true));
        r.setFamilyContact(str(body, "familyContact", ""));
        r.setRemark(str(body, "remark", ""));
        return reservationRepo.save(r);
    }

    @Transactional
    public ElderReservation arrangeReservation(Long id, Map<String, Object> body, String operator) {
        ElderReservation r = reservationRepo.findById(id).orElseThrow();
        r.setEscortStaff(str(body, "escortStaff", "大堂引导员"));
        r.setFamilyNote(str(body, "familyNote", r.getFamilyNote()));
        if (bool(body, "familyRemoteConfirmed", r.isFamilyRemoteConfirmed())) {
            r.setFamilyRemoteConfirmed(true);
        }
        r.setStatus(Enums.ReservationStatus.ARRANGED);
        reservationRepo.save(r);
        raiseEvent(Enums.EventType.ACCESSIBILITY, null, null, null,
                "老人预约已安排：" + r.getElderName() + "（" + r.getBusinessType().label
                        + "），陪同人=" + r.getEscortStaff()
                        + "，优先窗/材料预审/家属确认=" + r.isFamilyRemoteConfirmed(),
                Enums.Role.MANAGER, operator);
        return r;
    }

    @Transactional
    public ElderReservation familyConfirm(Long id, String note) {
        ElderReservation r = reservationRepo.findById(id).orElseThrow();
        r.setFamilyRemoteConfirmed(true);
        r.setFamilyNote(note == null ? "家属已视频确认" : note);
        return reservationRepo.save(r);
    }

    /** 老人到店：生成高优先级排队号并自动分流到敬老优先窗，附带陪同/预审标记 */
    @Transactional
    public QueueTicket reservationArrive(Long id, String operator) {
        ElderReservation r = reservationRepo.findById(id).orElseThrow();
        Map<String, Object> body = new HashMap<>();
        body.put("customerName", r.getElderName());
        body.put("phoneTail", r.getPhoneTail());
        body.put("customerType", "ELDER");
        body.put("source", "ELDER_RESERVATION");
        body.put("businessType", r.getBusinessType().name());
        body.put("riskLevel", "LOW");
        body.put("elder", true);
        body.put("wheelchair", r.isWheelchair());
        body.put("hearingGuide", r.isHearingGuide());
        body.put("materialScore", r.isMaterialPreReview() ? 90 : 70);

        QueueTicket t = createTicket(body, operator);
        t.setMaterialPreChecked(r.isMaterialPreReview());
        t.setFamilyConfirmed(r.isFamilyRemoteConfirmed());
        t.setFamilyNote(r.getFamilyNote());
        ticketRepo.save(t);

        Map<String, Object> suggestion = suggestRoute(t.getId());
        if (!Boolean.TRUE.equals(suggestion.get("blocked"))) {
            Map<String, Object> routeBody = new HashMap<>();
            routeBody.put("channel", suggestion.get("channel").toString());
            routeBody.put("windowNo", suggestion.get("windowNo"));
            routeBody.put("materialPreChecked", r.isMaterialPreReview());
            route(t.getId(), routeBody, operator);
        }
        r.setStatus(Enums.ReservationStatus.ARRIVED);
        r.setTicketNo(t.getTicketNo());
        reservationRepo.save(r);
        raiseEvent(Enums.EventType.ACCESSIBILITY, t.getId(), t.getTicketNo(), t.getAssignedWindow(),
                "预约老人到店，已安排陪同引导" + (r.isWheelchair() ? "（启用无障碍坡道）" : ""),
                Enums.Role.MANAGER, operator);
        return ticketRepo.findById(t.getId()).orElseThrow();
    }

    // ---------------- 协同事件 ----------------

    @Transactional
    public CollaborationEvent raiseEvent(Enums.EventType type, Long ticketId, String ticketNo,
                                         String windowNo, String description,
                                         Enums.Role ownerRole, String raisedBy) {
        CollaborationEvent e = new CollaborationEvent();
        e.setType(type);
        e.setTicketId(ticketId);
        e.setTicketNo(ticketNo);
        e.setWindowNo(windowNo);
        e.setDescription(description);
        e.setOwnerRole(ownerRole);
        e.setRaisedBy(raisedBy);
        e.setRaisedAt(LocalDateTime.now());
        return eventRepo.save(e);
    }

    @Transactional
    public CollaborationEvent raiseFromRequest(Map<String, Object> body, String operator) {
        Enums.EventType type = Enums.EventType.valueOf(str(body, "type", "GENERAL"));
        Long ticketId = body.get("ticketId") == null ? null : Long.valueOf(body.get("ticketId").toString());
        String windowNo = str(body, "windowNo", null);
        QueueTicket t = ticketId == null ? null : ticketRepo.findById(ticketId).orElse(null);

        // 事件与窗口/排队记录联动
        switch (type) {
            case TELLER_AWAY -> {
                if (windowNo != null) {
                    windowRepo.findByWindowNo(windowNo).ifPresent(w -> {
                        w.setStatus(Enums.WindowStatus.BREAK);
                        w.setNote(str(body, "description", "柜员临时离岗"));
                        windowRepo.save(w);
                    });
                }
            }
            case MACHINE_FAULT -> {
                if (windowNo != null) {
                    windowRepo.findByWindowNo(windowNo).ifPresent(w -> {
                        w.setStatus(Enums.WindowStatus.FAULT);
                        w.setNote(str(body, "description", "自助机故障，已报修"));
                        windowRepo.save(w);
                    });
                }
                // 在故障自助机上的客户改派柜台
                if (t != null) {
                    t.setChannel(Enums.Channel.COUNTER);
                    ServiceWindow fallback = pickWindow(Enums.WindowType.TELLER, t.getBusinessType());
                    if (fallback != null) {
                        t.setAssignedWindow(fallback.getWindowNo());
                        t.setStatus(Enums.TicketStatus.WAITING);
                    }
                    ticketRepo.save(t);
                }
                recalcAllLoads();
            }
            case MISSING_ID -> {
                if (t != null) {
                    t.setMaterialScore(40);
                    ticketRepo.save(t);
                }
            }
            case JUMP_QUEUE -> {
                // 投诉插队：登记投诉并给被插队客户补偿性优先
                if (t != null) {
                    t.setComplaint(str(body, "description", "客户投诉有人插队"));
                    t.setPriority(t.getPriority() + 15);
                    ticketRepo.save(t);
                }
            }
            default -> { }
        }
        Enums.Role owner = body.get("ownerRole") == null ? defaultOwner(type)
                : Enums.Role.valueOf(body.get("ownerRole").toString());
        return raiseEvent(type, ticketId, t == null ? null : t.getTicketNo(), windowNo,
                str(body, "description", type.label), owner, operator);
    }

    @Transactional
    public CollaborationEvent resolveEvent(Long eventId, String resolution, String operator) {
        CollaborationEvent e = eventRepo.findById(eventId).orElseThrow();
        e.setStatus(Enums.EventStatus.RESOLVED);
        e.setResolvedAt(LocalDateTime.now());
        e.setResolution(resolution);
        eventRepo.save(e);

        // 窗口类事件处置完成后恢复窗口
        if (e.getType() == Enums.EventType.TELLER_AWAY && e.getWindowNo() != null) {
            windowRepo.findByWindowNo(e.getWindowNo()).ifPresent(w -> {
                w.setStatus(Enums.WindowStatus.OPEN);
                w.setNote(null);
                windowRepo.save(w);
            });
        }
        if (e.getType() == Enums.EventType.MACHINE_FAULT && e.getWindowNo() != null) {
            windowRepo.findByWindowNo(e.getWindowNo()).ifPresent(w -> {
                w.setStatus(Enums.WindowStatus.OPEN);
                w.setNote(null);
                windowRepo.save(w);
            });
        }
        recalcAllLoads();
        return e;
    }

    private Enums.Role defaultOwner(Enums.EventType type) {
        return switch (type) {
            case FRAUD_CHECK, LARGE_CASH -> Enums.Role.TELLER;
            case MACHINE_FAULT, POWER_OUTAGE, ACCESSIBILITY -> Enums.Role.SECURITY;
            case JUMP_QUEUE -> Enums.Role.CUSTOMER_SERVICE;
            case TELLER_AWAY, PENSION_RUSH, LONG_WAIT, MISSING_ID -> Enums.Role.MANAGER;
            default -> Enums.Role.MANAGER;
        };
    }

    // ---------------- 网点状态：停电 / 养老金日 ----------------

    public BranchStatus branchStatus() {
        return branchRepo.findById(1L).orElseGet(() -> branchRepo.save(new BranchStatus()));
    }

    @Transactional
    public BranchStatus updateBranchStatus(Map<String, Object> body, String operator) {
        BranchStatus bs = branchStatus();
        boolean wasOutage = bs.isPowerOutage();
        if (body.containsKey("powerOutage")) bs.setPowerOutage(Boolean.parseBoolean(body.get("powerOutage").toString()));
        if (body.containsKey("pensionDay")) bs.setPensionDay(Boolean.parseBoolean(body.get("pensionDay").toString()));
        bs.setNotice(str(body, "notice", bs.getNotice()));
        branchRepo.save(bs);

        if (bs.isPowerOutage() && !wasOutage) {
            // 停电：挂起全部在办/等候单，窗口暂停叫号，安保维持秩序
            ticketRepo.findAll().forEach(t -> {
                if (t.getStatus() == Enums.TicketStatus.WAITING
                        || t.getStatus() == Enums.TicketStatus.CALLED
                        || t.getStatus() == Enums.TicketStatus.SERVING) {
                    t.setSuspended(true);
                    ticketRepo.save(t);
                }
            });
            raiseEvent(Enums.EventType.POWER_OUTAGE, null, null, null,
                    "网点突发停电：启用应急照明与手工叫号登记，安保维护现场，已挂起电子队列",
                    Enums.Role.SECURITY, operator);
        } else if (!bs.isPowerOutage() && wasOutage) {
            ticketRepo.findAll().forEach(t -> {
                if (t.isSuspended()) {
                    t.setSuspended(false);
                    ticketRepo.save(t);
                }
            });
            eventRepo.findByStatusInOrderByRaisedAtDesc(
                            List.of(Enums.EventStatus.OPEN, Enums.EventStatus.PROCESSING)).stream()
                    .filter(e -> e.getType() == Enums.EventType.POWER_OUTAGE)
                    .forEach(e -> resolveEvent(e.getId(), "供电恢复，电子叫号队列恢复，手工登记客户已回流", operator));
        }
        if (bs.isPensionDay()) {
            boolean exists = eventRepo.findAll().stream().anyMatch(e ->
                    e.getType() == Enums.EventType.PENSION_RUSH && e.getStatus() != Enums.EventStatus.RESOLVED);
            if (!exists) {
                raiseEvent(Enums.EventType.PENSION_RUSH, null, null, null,
                        "今日养老金集中发放：老年/养老金客户高峰，请增开敬老窗、加配引导与安保",
                        Enums.Role.MANAGER, operator);
            }
            ticketRepo.findByStatusOrderByPriorityDescCreatedAtAsc(Enums.TicketStatus.WAITING)
                    .stream()
                    .filter(t -> t.isElder() || t.getCustomerType() == Enums.CustomerType.PENSION)
                    .forEach(t -> {
                        t.setPriority(Math.max(t.getPriority(), 80));
                        ticketRepo.save(t);
                    });
        }
        return bs;
    }

    // ---------------- 长等待定时巡检 ----------------

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void scanLongWait() {
        LocalDateTime now = LocalDateTime.now();
        for (QueueTicket t : ticketRepo.findByStatusOrderByPriorityDescCreatedAtAsc(Enums.TicketStatus.WAITING)) {
            if (t.isSuspended()) continue;
            long waited = Duration.between(t.getCreatedAt(), now).toMinutes();
            if (waited >= LONG_WAIT_MINUTES) {
                t.setPriority(t.getPriority() + 2);
                ticketRepo.save(t);
                boolean exists = eventRepo.findByTicketIdOrderByRaisedAtAsc(t.getId()).stream()
                        .anyMatch(e -> e.getType() == Enums.EventType.LONG_WAIT
                                && e.getStatus() != Enums.EventStatus.RESOLVED);
                if (!exists) {
                    raiseEvent(Enums.EventType.LONG_WAIT, t.getId(), t.getTicketNo(), t.getAssignedWindow(),
                            "客户已等待" + waited + "分钟，超过" + LONG_WAIT_MINUTES
                                    + "分钟阈值，请大堂经理安抚并优先安排",
                            Enums.Role.MANAGER, "系统巡检");
                }
            }
        }
    }

    // ---------------- 现金库存 ----------------

    public BigDecimal totalCash() {
        return cashRepo.findAll().stream()
                .map(CashInventory::getAmountWan)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean consumeCash(BigDecimal need) {
        List<CashInventory> list = cashRepo.findAll();
        BigDecimal total = list.stream().map(CashInventory::getAmountWan).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(need) < 0) return false;
        BigDecimal remain = need;
        for (CashInventory c : list) {
            if (remain.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal take = c.getAmountWan().min(remain);
            c.setAmountWan(c.getAmountWan().subtract(take));
            c.setUpdatedAt(LocalDateTime.now());
            cashRepo.save(c);
            remain = remain.subtract(take);
        }
        return true;
    }

    // ---------------- 看板统计 ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("waiting", ticketRepo.countByStatus(Enums.TicketStatus.WAITING));
        d.put("called", ticketRepo.countByStatus(Enums.TicketStatus.CALLED));
        d.put("serving", ticketRepo.countByStatus(Enums.TicketStatus.SERVING));
        d.put("completed", ticketRepo.countByStatus(Enums.TicketStatus.COMPLETED));
        d.put("cancelled", ticketRepo.countByStatus(Enums.TicketStatus.CANCELLED));
        d.put("elderWaiting", ticketRepo.countByStatusAndElderTrue(Enums.TicketStatus.WAITING));
        d.put("openEvents", eventRepo.countByStatus(Enums.EventStatus.OPEN));
        d.put("cashTotalWan", totalCash());
        d.put("branch", branchStatus());

        List<OperationArchive> archives = archiveRepo.findAll();
        d.put("archiveCount", archives.size());
        d.put("avgWait", archives.stream().filter(a -> a.getWaitMinutes() > 0)
                .mapToInt(OperationArchive::getWaitMinutes).average().orElse(0));
        d.put("complaintCount", archives.stream().filter(OperationArchive::isComplaintRaised).count());
        d.put("misrouted", archiveRepo.countByChannelCorrectFalse());

        Map<String, Long> channelDist = new LinkedHashMap<>();
        for (Enums.Channel c : Enums.Channel.values()) {
            channelDist.put(c.name(), ticketRepo.countByChannel(c));
        }
        d.put("channelDistribution", channelDist);

        Map<String, Object> bizDist = new LinkedHashMap<>();
        for (Enums.BusinessType b : Enums.BusinessType.values()) {
            bizDist.put(b.name(), ticketRepo.findAll().stream()
                    .filter(x -> x.getBusinessType() == b).count());
        }
        d.put("businessDistribution", bizDist);
        d.put("windows", windowRepo.findAll());
        return d;
    }

    /** 叫号大屏（免登录） */
    @Transactional(readOnly = true)
    public Map<String, Object> displayBoard() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("branch", branchStatus());
        d.put("waiting", ticketRepo.findByStatusOrderByPriorityDescCreatedAtAsc(Enums.TicketStatus.WAITING)
                .stream().limit(12).toList());
        List<QueueTicket> active = ticketRepo.findByStatusInOrderByCreatedAtDesc(
                List.of(Enums.TicketStatus.CALLED, Enums.TicketStatus.SERVING));
        d.put("calling", active.stream().limit(8).toList());
        d.put("windows", windowRepo.findAll());
        d.put("cashTotalWan", totalCash());
        return d;
    }

    @Transactional
    public OperationArchive revisit(Long archiveId, Map<String, Object> body) {
        OperationArchive a = archiveRepo.findById(archiveId).orElseThrow();
        a.setRevisitDone(true);
        a.setRevisitResult(str(body, "revisitResult", "客户满意"));
        a.setSatisfaction(intVal(body, "satisfaction", 5));
        return archiveRepo.save(a);
    }

    @Transactional
    public QueueTicket updateTicket(Long id, Map<String, Object> body) {
        QueueTicket t = ticketRepo.findById(id).orElseThrow();
        if (body.containsKey("materialScore")) t.setMaterialScore(intVal(body, "materialScore", t.getMaterialScore()));
        if (body.containsKey("materialPreChecked")) t.setMaterialPreChecked(bool(body, "materialPreChecked", false));
        if (body.containsKey("complaint")) t.setComplaint(str(body, "complaint", ""));
        if (body.containsKey("familyConfirmed")) {
            t.setFamilyConfirmed(bool(body, "familyConfirmed", false));
            t.setFamilyNote(str(body, "familyNote", t.getFamilyNote()));
        }
        return ticketRepo.save(t);
    }

    private void recalcAllLoads() {
        for (ServiceWindow w : windowRepo.findAll()) {
            long load = ticketRepo.findByAssignedWindowAndStatusIn(w.getWindowNo(),
                    List.of(Enums.TicketStatus.WAITING, Enums.TicketStatus.CALLED, Enums.TicketStatus.SERVING)).size();
            w.setCurrentLoad((int) load);
            windowRepo.save(w);
        }
    }

    // ---- Map 取值工具 ----
    private static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v == null || v.toString().isBlank() ? def : v.toString();
    }
    private static boolean bool(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v == null ? def : Boolean.parseBoolean(v.toString());
    }
    private static int intVal(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        try { return v == null ? def : (int) Double.parseDouble(v.toString()); }
        catch (Exception e) { return def; }
    }
    private static BigDecimal dec(Map<String, Object> m, String k) {
        Object v = m.get(k);
        try { return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString()); }
        catch (Exception e) { return BigDecimal.ZERO; }
    }
}
