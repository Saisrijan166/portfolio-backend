package com.srijan.portfolio.service;

import com.srijan.portfolio.entity.RefreshToken;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.UnauthorizedException;
import com.srijan.portfolio.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    private final long refreshDurationMs = 7 * 24 * 60 * 60 * 1000;

    @Transactional
    public RefreshToken rotateRefreshToken(User user) {
        repository.deleteByUserId(user.getId());
        return createRefreshToken(user);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.deleteByUserId(userId);
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshDurationMs))
                .build();
        return repository.save(token);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            repository.delete(token);
            throw new UnauthorizedException("REFRESH_TOKEN_EXPIRED", "Refresh token expired");
        }
        return token;
    }

    public RefreshToken findByToken(String token) {
        return repository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));
    }
}
