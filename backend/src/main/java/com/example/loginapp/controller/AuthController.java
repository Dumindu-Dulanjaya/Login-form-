package com.example.loginapp.controller;

import com.example.loginapp.dto.LoginRequest;
import com.example.loginapp.dto.LoginResponse;
import com.example.loginapp.model.User;
import com.example.loginapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class AuthController {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByUsername(loginRequest.getUsername());

        if (!userOptional.isPresent()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        User user = userOptional.get();

        if (user.isAccountLocked()) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Account locked due to multiple failed login attempts. Please contact administrator.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        if (!user.getPassword().equals(loginRequest.getPassword())) {
            handleFailedLogin(user);
            Map<String, String> error = new HashMap<>();
            
            if (user.isAccountLocked()) {
                error.put("message", "Account locked due to multiple failed login attempts. Please contact administrator.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            int remainingAttempts = MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
            error.put("message", "Invalid credentials. " + remainingAttempts + " attempt(s) remaining.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        if (user.getNicNumber() != null && !user.getNicNumber().equals(loginRequest.getNicNumber())) {
            handleFailedLogin(user);
            Map<String, String> error = new HashMap<>();
            
            if (user.isAccountLocked()) {
                error.put("message", "Account locked due to multiple failed login attempts. Please contact administrator.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            int remainingAttempts = MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
            error.put("message", "Invalid credentials. " + remainingAttempts + " attempt(s) remaining.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        resetFailedAttempts(user);

        LoginResponse response = new LoginResponse(
            "Login successful",
            "dummy-jwt-token-" + System.currentTimeMillis(),
            user.getUsername()
        );
        return ResponseEntity.ok(response);
    }

    private void handleFailedLogin(User user) {
        int newFailedAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newFailedAttempts);

        if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        userRepository.save(user);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Username already exists");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setNicNumber(registerRequest.getNicNumber());
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);

        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

