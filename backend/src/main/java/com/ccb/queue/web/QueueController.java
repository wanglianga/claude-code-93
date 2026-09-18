package com.ccb.queue.web;

import com.ccb.queue.model.*;
import com.ccb.queue.repo.*;
import com.ccb.queue.security.AuthInterceptor;
import com.ccb.queue.service.QueueService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class QueueController {

    private final QueueService service;
    private final TicketRepository ticketRepo;
    private final WindowRepository windowRepo;
    private final EventRepository eventRepo;
    private final ReservationRepository reservationRepo;
    private final CashInventoryRepository cashRepo;
    private final ArchiveRepository archiveRepo;

    public QueueController(QueueService service, TicketRepository ticketRepo,
                           WindowRepository windowRepo, EventRepository eventRepo,
                           ReservationRepository reservationRepo, CashInventoryRepository cashRepo,
                           ArchiveRepository archiveRepo) {
        this.service = service;
        this.ticketRepo = ticketRepo;
        this.windowRepo = windowRepo;
        this.eventRepo = eventRepo;
        this.reservationRepo = reservationRepo;
        this.cashRepo = cashRepo;
        this.archiveRepo = archiveRepo;
    }

    // ---------------- 排队 ----------------

    @GetMapping("/tickets")
    public R tickets() {
        List<QueueTicket> list = new ArrayList<>(ticketRepo.findAll());
        list.sort(Comparator.comparing(QueueTicket::getId).reversed());
        return R.ok(list);
    }

    @GetMapping("/tickets/{id}")
    public R ticket(@PathVariable Long id) {
        return R.ok(ticketRepo.findById(id).orElseThrow());
    }

    @PostMapping("/tickets")
    public R create(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        return R.ok(service.createTicket(body, AuthInterceptor.current(req).getName()));
    }

    @PatchMapping("/tickets/{id}")
    public R patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.updateTicket(id, body));
    }

    @GetMapping("/tickets/{id}/route-suggest")
    public R suggest(@PathVariable Long id) {
        return R.ok(service.suggestRoute(id));
    }

    @PostMapping("/tickets/{id}/route")
    public R route(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest req) {
        return R.ok(service.route(id, body, AuthInterceptor.current(req).getName()));
    }

    @PostMapping("/windows/{windowNo}/call-next")
    public R callNext(@PathVariable String windowNo, HttpServletRequest req) {
        return R.ok(service.callNext(windowNo, AuthInterceptor.current(req).getName()));
    }

    @PostMapping("/tickets/{id}/start")
    public R start(@PathVariable Long id, HttpServletRequest req) {
        return R.ok(service.startServing(id, AuthInterceptor.current(req).getName()));
    }

    @PostMapping("/tickets/{id}/finish")
    public R finish(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                    HttpServletRequest req) {
        return R.ok(service.finish(id, body == null ? Map.of() : body,
                AuthInterceptor.current(req).getName()));
    }

    @PostMapping("/tickets/{id}/fraud-pass")
    public R fraudPass(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                       HttpServletRequest req) {
        String note = body == null ? "核验通过" : String.valueOf(body.getOrDefault("note", "核验通过"));
        return R.ok(service.fraudPass(id, note, AuthInterceptor.current(req).getName()));
    }

    @PostMapping("/tickets/{id}/fraud-intercept")
    public R fraudIntercept(@PathVariable Long id, @RequestBody Map<String, Object> body,
                            HttpServletRequest req) {
        String reason = String.valueOf(body.getOrDefault("reason", "疑似电信诈骗"));
        return R.ok(service.fraudIntercept(id, reason, AuthInterceptor.current(req).getName()));
    }

    // ---------------- 窗口 ----------------

    @GetMapping("/windows")
    public R windows() {
        return R.ok(windowRepo.findAll());
    }

    @PostMapping("/windows/{windowNo}/status")
    public R windowStatus(@PathVariable String windowNo, @RequestBody Map<String, Object> body,
                          HttpServletRequest req) {
        ServiceWindow w = windowRepo.findByWindowNo(windowNo).orElseThrow();
        w.setStatus(Enums.WindowStatus.valueOf(String.valueOf(body.get("status"))));
        w.setNote(body.get("note") == null ? null : body.get("note").toString());
        windowRepo.save(w);

        // 柜员临时离岗 / 设备故障同步登记协同事件
        Enums.WindowStatus st = w.getStatus();
        if (st == Enums.WindowStatus.BREAK || st == Enums.WindowStatus.FAULT) {
            Enums.EventType type = st == Enums.WindowStatus.BREAK
                    ? Enums.EventType.TELLER_AWAY : Enums.EventType.MACHINE_FAULT;
            Map<String, Object> eb = new HashMap<>();
            eb.put("type", type.name());
            eb.put("windowNo", windowNo);
            eb.put("description", w.getNote() == null ? type.label : w.getNote());
            service.raiseFromRequest(eb, AuthInterceptor.current(req).getName());
        }
        return R.ok(w);
    }

    // ---------------- 适老预约 ----------------

    @GetMapping("/reservations")
    public R reservations() {
        List<ElderReservation> list = reservationRepo.findAll();
        list.sort(Comparator.comparing(ElderReservation::getId).reversed());
        return R.ok(list);
    }

    @PostMapping("/reservations")
    public R createReservation(@RequestBody Map<String, Object> body) {
        return R.ok(service.createReservation(body));
    }

    @PostMapping("/reservations/{id}/arrange")
    public R arrange(@PathVariable Long id, @RequestBody Map<String, Object> body,
                     HttpServletRequest req) {
        return R.ok(service.arrangeReservation(id, body, AuthInterceptor.current(req).getName()));
    }

    @PostMapping("/reservations/{id}/family-confirm")
    public R familyConfirm(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String note = body == null ? "家属已视频确认" : String.valueOf(body.getOrDefault("note", "家属已视频确认"));
        return R.ok(service.familyConfirm(id, note));
    }

    @PostMapping("/reservations/{id}/arrive")
    public R arrive(@PathVariable Long id, HttpServletRequest req) {
        return R.ok(service.reservationArrive(id, AuthInterceptor.current(req).getName()));
    }

    // ---------------- 协同事件 ----------------

    @GetMapping("/events")
    public R events() {
        List<CollaborationEvent> list = new ArrayList<>(eventRepo.findAll());
        list.sort(Comparator.comparing(CollaborationEvent::getId).reversed());
        return R.ok(list);
    }

    @GetMapping("/tickets/{id}/events")
    public R ticketEvents(@PathVariable Long id) {
        return R.ok(eventRepo.findByTicketIdOrderByRaisedAtAsc(id));
    }

    @PostMapping("/events")
    public R raiseEvent(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        return R.ok(service.raiseFromRequest(body, AuthInterceptor.current(req).getName()));
    }

    @PostMapping("/events/{id}/resolve")
    public R resolve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body,
                     HttpServletRequest req) {
        String resolution = body == null ? "已处置完成"
                : String.valueOf(body.getOrDefault("resolution", "已处置完成"));
        return R.ok(service.resolveEvent(id, resolution, AuthInterceptor.current(req).getName()));
    }

    // ---------------- 现金库存 ----------------

    @GetMapping("/cash")
    public R cash() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", cashRepo.findAll());
        m.put("totalWan", service.totalCash());
        return R.ok(m);
    }

    @PostMapping("/cash/{id}/adjust")
    public R adjust(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        CashInventory c = cashRepo.findById(id).orElseThrow();
        BigDecimal delta = new BigDecimal(body.get("deltaWan").toString());
        c.setAmountWan(c.getAmountWan().add(delta).max(BigDecimal.ZERO));
        c.setUpdatedAt(LocalDateTime.now());
        return R.ok(cashRepo.save(c));
    }

    // ---------------- 网点状态 ----------------

    @GetMapping("/branch")
    public R branch() {
        return R.ok(service.branchStatus());
    }

    @PostMapping("/branch")
    public R setBranch(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        return R.ok(service.updateBranchStatus(body, AuthInterceptor.current(req).getName()));
    }

    // ---------------- 运营档案 ----------------

    @GetMapping("/archives")
    public R archives() {
        return R.ok(archiveRepo.findAllByOrderByArchivedAtDesc());
    }

    @PostMapping("/archives/{id}/revisit")
    public R revisit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(service.revisit(id, body));
    }

    // ---------------- 看板 / 大屏（免登录） ----------------

    @GetMapping("/dashboard")
    public R dashboard() {
        return R.ok(service.dashboard());
    }

    @GetMapping("/display/board")
    public R board() {
        return R.ok(service.displayBoard());
    }
}
