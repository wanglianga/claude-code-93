package com.ccb.queue.web;

import com.ccb.queue.model.Enums;
import com.ccb.queue.model.User;
import com.ccb.queue.repo.UserRepository;
import com.ccb.queue.security.AuthInterceptor;
import com.ccb.queue.security.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class AuthMetaController {

    private final UserRepository userRepo;
    private final TokenStore tokenStore;

    public AuthMetaController(UserRepository userRepo, TokenStore tokenStore) {
        this.userRepo = userRepo;
        this.tokenStore = tokenStore;
    }

    public record LoginReq(String username, String password) {}

    @PostMapping("/auth/login")
    public R login(@RequestBody LoginReq req) {
        Optional<User> u = userRepo.findByUsername(req.username() == null ? "" : req.username());
        if (u.isEmpty() || !u.get().getPassword().equals(req.password())) {
            return R.fail("用户名或密码错误");
        }
        User user = u.get();
        String token = tokenStore.issue(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        data.put("role", user.getRole().name());
        data.put("windowNo", user.getWindowNo());
        return R.ok(data);
    }

    @GetMapping("/auth/me")
    public R me(HttpServletRequest request) {
        return R.ok(AuthInterceptor.current(request));
    }

    /** 业务字典（免登录） */
    @GetMapping("/meta/enums")
    public R enums() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roles", describe(Enums.Role.values()));
        data.put("customerTypes", describe(Enums.CustomerType.values()));
        data.put("riskLevels", describe(Enums.RiskLevel.values()));
        data.put("sources", describe(Enums.TicketSource.values()));
        data.put("businessTypes", describe(Enums.BusinessType.values()));
        data.put("channels", describe(Enums.Channel.values()));
        data.put("windowTypes", describe(Enums.WindowType.values()));
        data.put("windowStatuses", describe(Enums.WindowStatus.values()));
        data.put("eventTypes", describe(Enums.EventType.values()));
        data.put("eventStatuses", describe(Enums.EventStatus.values()));
        data.put("ticketStatuses", describe(Enums.TicketStatus.values()));
        data.put("reservationStatuses", describe(Enums.ReservationStatus.values()));
        return R.ok(data);
    }

    private List<Map<String, String>> describe(Enum<?>[] values) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Enum<?> v : values) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("value", v.name());
            String label = v.name();
            try {
                label = (String) v.getClass().getField("label").get(v);
            } catch (Exception ignore) {
                // 无 label 字段的枚举直接用枚举名
            }
            m.put("label", label);
            list.add(m);
        }
        return list;
    }
}
