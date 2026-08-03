package com.agenthub.auth.service;

import com.agenthub.auth.dto.*;
import com.agenthub.auth.entity.AuthUser;
import com.agenthub.auth.exception.AuthenticationException;
import com.agenthub.auth.exception.CustomerNotFoundException;
import com.agenthub.auth.repository.AuthUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomerServiceClient customerServiceClient;

    public AuthService(AuthUserRepository authUserRepository,
                      PasswordEncoder passwordEncoder,
                      JwtService jwtService,
                      CustomerServiceClient customerServiceClient) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.customerServiceClient = customerServiceClient;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        logger.info("Registration attempt for username: {}", request.getUsername());

        // Validate password strength
        if (!isPasswordValid(request.getPassword())) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain uppercase, lowercase, number, and special character"
            );
        }

        // Check if username already exists
        if (authUserRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Only validate customer if customerId is provided
        if (request.getCustomerId() != null) {
            // Check if customer already has an auth account
            if (authUserRepository.existsByCustomerId(request.getCustomerId())) {
                throw new IllegalArgumentException("Customer already has an authentication account");
            }

            // Validate customer exists
            if (!customerServiceClient.validateCustomerExists(request.getCustomerId())) {
                throw new CustomerNotFoundException("Customer ID " + request.getCustomerId() + " not found");
            }
        }

        // Create new auth user
        AuthUser authUser = new AuthUser()
                .setCustomerId(request.getCustomerId())
                .setUsername(request.getUsername())
                .setEmail(request.getEmail())
                .setPasswordHash(passwordEncoder.encode(request.getPassword()))
                .setEnabled(true)
                .setAccountLocked(false)
                .setFailedLoginAttempts(0);

        authUser = authUserRepository.save(authUser);
        logger.info("User registered successfully: {}", authUser.getUsername());

        return new RegisterResponse(
                authUser.getId(),
                authUser.getUsername(),
                authUser.getEmail(),
                authUser.getCustomerId()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        logger.info("Login attempt for username: {}", request.getUsername());

        AuthUser authUser = authUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        // Check if account is locked
        if (authUser.getAccountLocked()) {
            throw new AuthenticationException("Account is locked due to too many failed login attempts");
        }

        // Check if account is enabled
        if (!authUser.getEnabled()) {
            throw new AuthenticationException("Account is disabled");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), authUser.getPasswordHash())) {
            handleFailedLogin(authUser);
            throw new AuthenticationException("Invalid username or password");
        }

        // Reset failed login attempts and update last login
        authUser.setFailedLoginAttempts(0)
                .setLastLoginAt(LocalDateTime.now());
        authUserRepository.save(authUser);

        // Generate JWT token
        String token = jwtService.generateToken(authUser);
        logger.info("User logged in successfully: {}", authUser.getUsername());

        return new LoginResponse(
                token,
                jwtService.getExpirationSeconds(),
                authUser.getUsername(),
                authUser.getCustomerId()
        );
    }

    @Transactional
    public UserInfoResponse getUserInfo(String username) {
        AuthUser authUser = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        return new UserInfoResponse(
                authUser.getId(),
                authUser.getUsername(),
                authUser.getEmail(),
                authUser.getCustomerId()
        );
    }

    @Transactional
    public MessageResponse changePassword(String username, ChangePasswordRequest request) {
        logger.info("Password change attempt for username: {}", username);

        AuthUser authUser = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), authUser.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        // Validate new password strength
        if (!isPasswordValid(request.getNewPassword())) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain uppercase, lowercase, number, and special character"
            );
        }

        // Update password
        authUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        authUserRepository.save(authUser);

        logger.info("Password changed successfully for username: {}", username);
        return new MessageResponse("Password changed successfully");
    }

    private void handleFailedLogin(AuthUser authUser) {
        int failedAttempts = authUser.getFailedLoginAttempts() + 1;
        authUser.setFailedLoginAttempts(failedAttempts);

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            authUser.setAccountLocked(true);
            logger.warn("Account locked for user: {} due to {} failed attempts",
                    authUser.getUsername(), failedAttempts);
        }

        authUserRepository.save(authUser);
    }

    private boolean isPasswordValid(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
