package com.srijan.portfolio.service;

import com.srijan.portfolio.entity.RefreshToken;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.UnauthorizedException;
import com.srijan.portfolio.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record IssuedRefreshToken(RefreshToken refreshToken, String rawToken) {}

    private final RefreshTokenRepository repository;
    private final PasswordEncoder passwordEncoder;

    private final long refreshDurationMs = 7L * 24 * 60 * 60 * 1000;

    @Transactional
    public IssuedRefreshToken issueRefreshToken(User user, String deviceInfo, String ipAddress) {
        String tokenId = UUID.randomUUID().toString();
        String secret = UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
        String rawToken = tokenId + "." + secret;

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenId(tokenId)
                .tokenHash(passwordEncoder.encode(rawToken))
                .expiryDate(Instant.now().plusMillis(refreshDurationMs))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .revoked(false)
                .build();

        return new IssuedRefreshToken(repository.save(token), rawToken);
    }

    @Transactional
    public IssuedRefreshToken rotateRefreshToken(String presentedToken, String deviceInfo, String ipAddress) {
        RefreshToken current = verifyUsable(findByTokenValue(presentedToken), presentedToken);
        current.setRevoked(true);
        repository.save(current);
        return issueRefreshToken(current.getUser(), deviceInfo, ipAddress);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.findAll().stream()
                .filter(token -> token.getUser() != null && userId.equals(token.getUser().getId()) && !token.isRevoked())
                .forEach(token -> token.setRevoked(true));
    }

    @Transactional
    public void revokeByToken(String tokenValue) {
        RefreshToken token = findByTokenValue(tokenValue);
        token.setRevoked(true);
        repository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyUsable(RefreshToken token, String presentedToken) {
        if (token.isRevoked()) {
            throw new UnauthorizedException("REFRESH_TOKEN_REVOKED", "Refresh token is revoked");
        }

        if (token.getExpiryDate() != null && token.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthorizedException("REFRESH_TOKEN_EXPIRED", "Refresh token expired");
        }

        if (token.getTokenHash() != null && !passwordEncoder.matches(presentedToken, token.getTokenHash())) {
            throw new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid");
        }

        if (token.getTokenHash() == null && token.getToken() != null && !token.getToken().equals(presentedToken)) {
            throw new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid");
        }

        return token;
    }

    @Transactional(readOnly = true)
    public RefreshToken findByTokenValue(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid");
        }

        if (presentedToken.contains(".")) {
            String tokenId = presentedToken.substring(0, presentedToken.indexOf('.'));
            return repository.findByTokenId(tokenId)
                    .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));
        }

        return repository.findByToken(presentedToken)
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid"));
    }
}
