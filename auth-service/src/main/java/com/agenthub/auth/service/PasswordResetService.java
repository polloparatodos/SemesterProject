package com.agenthub.auth.service;

import com.agenthub.auth.dto.ForgotPasswordRequest;
import com.agenthub.auth.dto.ForgotPasswordResponse;
import com.agenthub.auth.dto.MessageResponse;
import com.agenthub.auth.dto.ResetPasswordRequest;
import com.agenthub.auth.entity.AuthUser;
import com.agenthub.auth.entity.PasswordResetToken;
import com.agenthub.auth.exception.AuthenticationException;
import com.agenthub.auth.exception.InvalidTokenException;
import com.agenthub.auth.repository.AuthUserRepository;
import com.agenthub.auth.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int RESET_TOKEN_EXPIRATION_HOURS = 1;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    private final AuthUserRepository authUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(AuthUserRepository authUserRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        logger.info("Password reset requested for email: {}", request.getEmail());

        AuthUser authUser = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Email not found"));

        // Delete any existing reset tokens for this user
        passwordResetTokenRepository.deleteByAuthUserId(authUser.getId());

        // Generate new reset token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(RESET_TOKEN_EXPIRATION_HOURS);

        PasswordResetToken resetToken = new PasswordResetToken()
                .setAuthUser(authUser)
                .setToken(token)
                .setExpiresAt(expiresAt)
                .setUsed(false);

        passwordResetTokenRepository.save(resetToken);
        logger.info("Password reset token generated for user: {}", authUser.getUsername());

        return new ForgotPasswordResponse(
                token,
                expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "Password reset token generated. Use this token to reset your password within 1 hour."
        );
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        logger.info("Password reset attempt with token");

        // Find reset token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getResetToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        // Check if token has been used
        if (resetToken.getUsed()) {
            throw new InvalidTokenException("Reset token has already been used");
        }

        // Check if token has expired
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Reset token has expired");
        }

        // Validate new password strength
        if (!isPasswordValid(request.getNewPassword())) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain uppercase, lowercase, number, and special character"
            );
        }

        // Update password
        AuthUser authUser = resetToken.getAuthUser();
        authUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()))
                .setFailedLoginAttempts(0)
                .setAccountLocked(false);
        authUserRepository.save(authUser);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        logger.info("Password reset successful for user: {}", authUser.getUsername());
        return new MessageResponse("Password reset successful");
    }

    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Cleaning up expired password reset tokens");
        passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private boolean isPasswordValid(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
