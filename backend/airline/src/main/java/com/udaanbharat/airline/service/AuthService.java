package com.udaanbharat.airline.service;

import com.udaanbharat.airline.dto.AuthResponse;
import com.udaanbharat.airline.dto.LoginRequest;
import com.udaanbharat.airline.dto.RegisterRequest;
import com.udaanbharat.airline.entity.Passenger;
import com.udaanbharat.airline.repository.PassengerRepository;
import com.udaanbharat.airline.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final PassengerRepository passengerRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(PassengerRepository passengerRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder) {
        this.passengerRepository = passengerRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest req) {
        if (passengerRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered.");
        }
        Passenger p = new Passenger();
        p.setName(req.getName());
        p.setEmail(req.getEmail());
        p.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        p.setPhone(req.getPhone());
        p.setAge(0);
        p.setGender("");
        passengerRepository.save(p);

        String token = jwtUtil.generateToken(p.getEmail(), p.getPassengerId());
        return new AuthResponse(token, p.getPassengerId(), p.getName(), p.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        Passenger p = passengerRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password."));

        if (!passwordEncoder.matches(req.getPassword(), p.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password.");
        }
        String token = jwtUtil.generateToken(p.getEmail(), p.getPassengerId());
        return new AuthResponse(token, p.getPassengerId(), p.getName(), p.getEmail());
    }

    public Passenger getProfile(String email) {
        return passengerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }
}