package com.agenthub.auth.controller;

import com.agenthub.auth.dto.*;
import com.agenthub.auth.security.JwtKeyProvider;
import com.agenthub.auth.service.AuthService;
import com.agenthub.auth.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final JwtKeyProvider jwtKeyProvider;

    public AuthController(AuthService authService,
                         PasswordResetService passwordResetService,
                         JwtKeyProvider jwtKeyProvider) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.jwtKeyProvider = jwtKeyProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        MessageResponse response = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey() {
        PublicKeyResponse response = new PublicKeyResponse(
                jwtKeyProvider.getPublicKeyAsString(),
                "RS256"
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        UserInfoResponse response = authService.getUserInfo(username);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        MessageResponse response = authService.changePassword(username, request);
        return ResponseEntity.ok(response);
    }
}
