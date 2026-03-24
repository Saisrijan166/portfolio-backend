package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.entity.PlatformFeedback;
import com.srijan.portfolio.entity.PortfolioFeedback;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.PlatformFeedbackRepository;
import com.srijan.portfolio.repository.PortfolioFeedbackRepository;
import com.srijan.portfolio.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final String PUBLIC_VISITOR_SOURCE = "PUBLIC_VISITOR";
    private static final String PORTFOLIO_OWNER_SOURCE = "PORTFOLIO_OWNER";
    private static final int MAX_PAGE_SIZE = 50;

    private final UserRepository userRepository;
    private final PortfolioFeedbackRepository portfolioFeedbackRepository;
    private final PlatformFeedbackRepository platformFeedbackRepository;
    private final FeedbackEmailService feedbackEmailService;

    @Value("${feedback.hash.secret:feedback-default-secret}")
    private String feedbackHashSecret;

    @Value("${security.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    @Transactional
    public FeedbackSubmissionResponse submitPublicPortfolioFeedback(
            String username,
            FeedbackSubmitRequest request,
            String visitorToken,
            HttpServletRequest httpRequest
    ) {
        User owner = findUserByUsername(username);
        String clientIp = resolveClientIp(httpRequest);
        String tokenHash = visitorTokenHash(visitorToken, clientIp);
        PortfolioFeedback feedback = portfolioFeedbackRepository
                .findByOwnerIdAndVisitorTokenHash(owner.getId(), tokenHash)
                .orElseGet(() -> PortfolioFeedback.builder()
                        .owner(owner)
                        .visitorTokenHash(tokenHash)
                        .build());

        boolean updatedExisting = feedback.getId() != null;
        boolean hadMessage = hasText(feedback.getMessage());

        feedback.setVisitorIp(hashIdentifier("ip", clientIp));
        if (request.getRating() != null) {
            feedback.setRating(request.getRating());
        }
        if (hasText(request.getMessage())) {
            feedback.setMessage(clean(request.getMessage()));
        }

        PortfolioFeedback saved = portfolioFeedbackRepository.save(feedback);
        if (!updatedExisting || (!hadMessage && hasText(saved.getMessage()))) {
            feedbackEmailService.sendPortfolioFeedbackNotification(owner, saved);
        }

        return FeedbackSubmissionResponse.builder()
                .id(saved.getId())
                .rating(saved.getRating())
                .message(saved.getMessage())
                .updatedExisting(updatedExisting)
                .messageStored(hasText(saved.getMessage()))
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional
    public FeedbackSubmissionResponse submitPublicPlatformFeedback(
            String username,
            FeedbackSubmitRequest request,
            String visitorToken,
            HttpServletRequest httpRequest
    ) {
        User owner = findUserByUsername(username);
        String clientIp = resolveClientIp(httpRequest);
        String tokenHash = visitorTokenHash(visitorToken, clientIp);

        PlatformFeedback feedback = platformFeedbackRepository
                .findByPortfolioOwnerIdAndVisitorTokenHashAndSourceType(owner.getId(), tokenHash, PUBLIC_VISITOR_SOURCE)
                .orElseGet(() -> PlatformFeedback.builder()
                        .portfolioOwner(owner)
                        .sourceType(PUBLIC_VISITOR_SOURCE)
                        .visitorTokenHash(tokenHash)
                        .build());

        boolean updatedExisting = feedback.getId() != null;

        feedback.setVisitorIp(hashIdentifier("ip", clientIp));
        if (request.getRating() != null) {
            feedback.setRating(request.getRating());
        }
        if (hasText(request.getMessage())) {
            feedback.setMessage(clean(request.getMessage()));
        }

        PlatformFeedback saved = platformFeedbackRepository.save(feedback);
        feedbackEmailService.sendPlatformFeedbackNotification("public visitor on @" + owner.getUsername(), saved);

        return FeedbackSubmissionResponse.builder()
                .id(saved.getId())
                .rating(saved.getRating())
                .message(saved.getMessage())
                .updatedExisting(updatedExisting)
                .messageStored(hasText(saved.getMessage()))
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ReceivedFeedbackPageDto getReceivedFeedback(String username, int page, int size) {
        User owner = findUserByUsername(username);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<PortfolioFeedback> feedbackPage = portfolioFeedbackRepository.findByOwnerIdOrderByUpdatedAtDesc(
                owner.getId(),
                PageRequest.of(safePage - 1, safeSize)
        );

        Double averageRating = portfolioFeedbackRepository.findAverageRatingByOwnerId(owner.getId());
        long count = portfolioFeedbackRepository.countByOwnerId(owner.getId());

        return ReceivedFeedbackPageDto.builder()
                .items(feedbackPage.getContent().stream().map(this::toReceivedDto).toList())
                .page(safePage)
                .size(safeSize)
                .totalPages(feedbackPage.getTotalPages())
                .totalItems(feedbackPage.getTotalElements())
                .feedbackCount(count)
                .averageRating(averageRating == null ? 0D : roundAverage(averageRating))
                .build();
    }

    @Transactional(readOnly = true)
    public AdminFeedbackDashboardDto getAdminFeedbackDashboard(String username, int page, int size) {
        User user = findUserByUsername(username);

        FeedbackStateDto platform = platformFeedbackRepository.findBySubmittedByUserId(user.getId())
                .map(this::toStateDto)
                .orElse(null);

        return AdminFeedbackDashboardDto.builder()
                .received(getReceivedFeedback(username, page, size))
                .platform(platform)
                .build();
    }

    @Transactional
    public FeedbackSubmissionResponse submitPlatformFeedback(String username, FeedbackSubmitRequest request) {
        User user = findUserByUsername(username);

        PlatformFeedback feedback = platformFeedbackRepository.findBySubmittedByUserId(user.getId())
                .orElseGet(() -> PlatformFeedback.builder()
                        .portfolioOwner(user)
                        .submittedByUser(user)
                        .sourceType(PORTFOLIO_OWNER_SOURCE)
                        .build());

        boolean updatedExisting = feedback.getId() != null;

        if (request.getRating() != null) {
            feedback.setRating(request.getRating());
        }
        if (hasText(request.getMessage())) {
            feedback.setMessage(clean(request.getMessage()));
        }

        PlatformFeedback saved = platformFeedbackRepository.save(feedback);
        feedbackEmailService.sendPlatformFeedbackNotification("portfolio owner @" + user.getUsername(), saved);

        return FeedbackSubmissionResponse.builder()
                .id(saved.getId())
                .rating(saved.getRating())
                .message(saved.getMessage())
                .updatedExisting(updatedExisting)
                .messageStored(hasText(saved.getMessage()))
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    private ReceivedFeedbackDto toReceivedDto(PortfolioFeedback feedback) {
        return ReceivedFeedbackDto.builder()
                .id(feedback.getId())
                .rating(feedback.getRating())
                .message(feedback.getMessage())
                .submittedAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    private FeedbackStateDto toStateDto(PortfolioFeedback feedback) {
        return FeedbackStateDto.builder()
                .rating(feedback.getRating())
                .message(feedback.getMessage())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    private FeedbackStateDto toStateDto(PlatformFeedback feedback) {
        return FeedbackStateDto.builder()
                .rating(feedback.getRating())
                .message(feedback.getMessage())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(clean(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = trustProxyHeaders ? request.getHeader("X-Forwarded-For") : null;
        if (hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String visitorTokenHash(String visitorToken, String clientIp) {
        String stableKey = hasText(visitorToken) ? clean(visitorToken) : "ip:" + clean(clientIp);
        return hashIdentifier("visitor", stableKey);
    }

    private double roundAverage(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String hashIdentifier(String namespace, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(feedbackHashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((namespace + ":" + value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to hash feedback identifier");
        }
    }
}
