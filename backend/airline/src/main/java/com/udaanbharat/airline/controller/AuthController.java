package com.udaanbharat.airline.controller;

import com.udaanbharat.airline.dto.AuthResponse;
import com.udaanbharat.airline.dto.LoginRequest;
import com.udaanbharat.airline.dto.RegisterRequest;
import com.udaanbharat.airline.entity.Passenger;
import com.udaanbharat.airline.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            AuthResponse response = authService.register(req);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            AuthResponse response = authService.login(req);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/auth/me  — returns profile of currently logged-in user
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal String email) {
        try {
            Passenger p = authService.getProfile(email);
            return ResponseEntity.ok(Map.of(
                "userId", p.getPassengerId(),
                "name", p.getName(),
                "email", p.getEmail(),
                "phone", p.getPhone() != null ? p.getPhone() : ""
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}