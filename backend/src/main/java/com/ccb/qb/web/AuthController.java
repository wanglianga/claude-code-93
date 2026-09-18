package com.ccb.qb.web;

import com.ccb.qb.dto.Dtos;
import com.ccb.qb.entity.User;
import com.ccb.qb.repo.UserRepository;
import com.ccb.qb.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Dtos.LoginRequest req) {
        User u = users.findByUsername(req.username()).orElse(null);
        if (u == null || !u.isEnabled() || !encoder.matches(req.password(), u.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }
        String token = jwt.generate(u.getId(), u.getUsername(), u.getRole().name());
        return ResponseEntity.ok(new Dtos.LoginResponse(token, u.getId(), u.getUsername(),
                u.getDisplayName(), u.getRole().name(), u.getSkill(), u.getCounterId()));
    }
}
