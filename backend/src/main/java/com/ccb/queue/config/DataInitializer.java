package com.ccb.queue.config;

import com.ccb.queue.model.*;
import com.ccb.queue.repo.*;
import com.ccb.queue.service.QueueService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 首次启动演示数据：账号、窗口、现金库存、样例排队单与老人预约 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final WindowRepository windowRepo;
    private final CashInventoryRepository cashRepo;
    private final BranchStatusRepository branchRepo;
    private final TicketRepository ticketRepo;
    private final ReservationRepository reservationRepo;
    private final QueueService queueService;

    public DataInitializer(UserRepository userRepo, WindowRepository windowRepo,
                           CashInventoryRepository cashRepo, BranchStatusRepository branchRepo,
                           TicketRepository ticketRepo, ReservationRepository reservationRepo,
                           QueueService queueService) {
        this.userRepo = userRepo;
        this.windowRepo = windowRepo;
        this.cashRepo = cashRepo;
        this.branchRepo = branchRepo;
        this.ticketRepo = ticketRepo;
        this.reservationRepo = reservationRepo;
        this.queueService = queueService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        // ---- 账号（密码统一 123456） ----
        createUser("manager01", "陈静", Enums.Role.MANAGER, null);
        createUser("teller01", "王磊", Enums.Role.TELLER, "W1");
        createUser("teller02", "李婷", Enums.Role.TELLER, "W2");
        createUser("teller03", "赵强", Enums.Role.TELLER, "W3");
        createUser("teller04", "孙敏", Enums.Role.TELLER, "P1");
        createUser("wealth01", "周琳", Enums.Role.WEALTH_MANAGER, "L1");
        createUser("security01", "吴刚", Enums.Role.SECURITY, null);
        createUser("cs01", "郑洁", Enums.Role.CUSTOMER_SERVICE, null);
        createUser("viewer01", "演示观察员", Enums.Role.VIEWER, null);

        // ---- 窗口 ----
        createWindow("W1", Enums.WindowType.TELLER, "王磊",
                "LARGE_CASH,TRANSFER,REPORT_LOSS,OPEN_ACCOUNT,SOCIAL_CARD");
        createWindow("W2", Enums.WindowType.TELLER, "李婷",
                "OPEN_ACCOUNT,SOCIAL_CARD,TRANSFER");
        createWindow("W3", Enums.WindowType.TELLER, "赵强",
                "TRANSFER,REPORT_LOSS,OPEN_ACCOUNT");
        createWindow("P1", Enums.WindowType.PRIORITY, "孙敏",
                "SOCIAL_CARD,OPEN_ACCOUNT,TRANSFER,LARGE_CASH,REPORT_LOSS,WEALTH_CONSULT");
        createWindow("L1", Enums.WindowType.WEALTH, "周琳", "WEALTH_CONSULT");
        createWindow("M1", Enums.WindowType.SELF_SERVICE, "大堂协助", "");
        createWindow("M2", Enums.WindowType.SELF_SERVICE, "大堂协助", "");

        // ---- 现金库存（万元） ----
        cashRepo.save(new CashInventory("100元券", new BigDecimal("120.00")));
        cashRepo.save(new CashInventory("50元券", new BigDecimal("30.00")));
        cashRepo.save(new CashInventory("20元及以下", new BigDecimal("10.00")));

        branchRepo.save(new BranchStatus());

        // ---- 老人预约 ----
        ElderReservation r1 = new ElderReservation();
        r1.setElderName("田秀珍");
        r1.setPhoneTail("8821");
        r1.setBusinessType(Enums.BusinessType.SOCIAL_CARD);
        r1.setReserveTime(LocalDateTime.now().plusHours(2));
        r1.setWheelchair(true);
        r1.setHearingGuide(false);
        r1.setFamilyContact("家属 138****6677");
        r1.setRemark("需无障碍坡道与陪同领号");
        reservationRepo.save(r1);

        ElderReservation r2 = new ElderReservation();
        r2.setElderName("黄保国");
        r2.setPhoneTail("3345");
        r2.setBusinessType(Enums.BusinessType.LARGE_CASH);
        r2.setReserveTime(LocalDateTime.now().plusHours(1));
        r2.setEscortStaff("安保吴刚");
        r2.setFamilyRemoteConfirmed(true);
        r2.setFamilyNote("女儿黄海燕视频确认办理 8 万元现金取款");
        r2.setStatus(Enums.ReservationStatus.ARRANGED);
        reservationRepo.save(r2);

        // ---- 样例排队单 ----
        seedTicket(Map.of(
                "customerName", "刘桂兰", "phoneTail", "1122",
                "customerType", "ELDER", "source", "ELDER_RESERVATION",
                "businessType", "SOCIAL_CARD", "riskLevel", "LOW",
                "elder", "true", "hearingGuide", "true",
                "materialScore", "75"), 25);
        seedTicket(Map.of(
                "customerName", "张建国", "phoneTail", "7788",
                "customerType", "NORMAL", "source", "WALK_IN",
                "businessType", "OPEN_ACCOUNT", "riskLevel", "LOW",
                "materialScore", "95"), 8);
        seedTicket(Map.of(
                "customerName", "王海燕", "phoneTail", "9001",
                "customerType", "NORMAL", "source", "APP",
                "businessType", "TRANSFER", "riskLevel", "HIGH",
                "amountWan", "50", "materialScore", "100"), 5);
        seedTicket(Map.of(
                "customerName", "马德福", "phoneTail", "2266",
                "customerType", "PENSION", "source", "WALK_IN",
                "businessType", "SOCIAL_CARD", "riskLevel", "LOW",
                "elder", "true", "materialScore", "85"), 3);
        seedTicket(Map.of(
                "customerName", "李国庆", "phoneTail", "5566",
                "customerType", "VIP", "source", "APP",
                "businessType", "WEALTH_CONSULT", "riskLevel", "MEDIUM",
                "amountWan", "100", "materialScore", "100",
                "estimatedMinutes", "40"), 2);
    }

    private void seedTicket(Map<String, String> body, int createdMinutesAgo) {
        QueueTicket t = queueService.createTicket(new java.util.HashMap<>(body), "系统初始化");
        t.setCreatedAt(LocalDateTime.now().minusMinutes(createdMinutesAgo));
        ticketRepo.save(t);
    }

    private void createUser(String username, String name, Enums.Role role, String windowNo) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("123456");
        u.setName(name);
        u.setRole(role);
        u.setWindowNo(windowNo);
        userRepo.save(u);
    }

    private void createWindow(String no, Enums.WindowType type, String staff, String skills) {
        ServiceWindow w = new ServiceWindow();
        w.setWindowNo(no);
        w.setType(type);
        w.setStaffName(staff);
        w.setSkills(skills);
        w.setStatus(Enums.WindowStatus.OPEN);
        windowRepo.save(w);
    }
}
