package com.srijan.portfolio.service;

import com.srijan.portfolio.dto.*;
import com.srijan.portfolio.entity.PlatformFeedback;
import com.srijan.portfolio.entity.PortfolioFeedback;
import com.srijan.portfolio.entity.User;
import com.srijan.portfolio.exception.ResourceNotFoundException;
import com.srijan.portfolio.repository.PlatformFeedbackRepository;
import com.srijan.portfolio.repository.PortfolioFeedbackRepository;
import com.srijan.portfolio.repository.UserRepository;
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

    @Transactional
    public FeedbackSubmissionResponse submitPublicPortfolioFeedback(
            String username,
            FeedbackSubmitRequest request,
            String visitorToken
    ) {
        User owner = findUserByUsername(username);
        PortfolioFeedback ratingFeedback = null;
        PortfolioFeedback messageFeedback = null;
        boolean ratingUpdatedExisting = false;

        if (request.getRating() != null) {
            String tokenHash = visitorTokenHash(visitorToken);
            ratingFeedback = portfolioFeedbackRepository
                    .findByOwnerIdAndVisitorTokenHash(owner.getId(), tokenHash)
                    .orElseGet(() -> PortfolioFeedback.builder()
                            .owner(owner)
                            .visitorTokenHash(tokenHash)
                            .build());
            ratingUpdatedExisting = ratingFeedback.getId() != null;
            ratingFeedback.setRating(request.getRating());
            ratingFeedback = portfolioFeedbackRepository.save(ratingFeedback);
        }

        if (hasText(request.getMessage())) {
            messageFeedback = portfolioFeedbackRepository.save(
                    PortfolioFeedback.builder()
                            .owner(owner)
                            .submitterName(cleanName(request.getName()))
                            .message(clean(request.getMessage()))
                            .build()
            );
            feedbackEmailService.sendPortfolioFeedbackNotification(owner, messageFeedback);
        } else if (ratingFeedback != null && !ratingUpdatedExisting) {
            feedbackEmailService.sendPortfolioFeedbackNotification(owner, ratingFeedback);
        }

        PortfolioFeedback responseTarget = messageFeedback != null ? messageFeedback : ratingFeedback;
        if (responseTarget == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least a rating or a message");
        }

        FeedbackSubmissionResponse response = toSubmissionResponse(responseTarget, messageFeedback == null && ratingUpdatedExisting);
        Double avg = portfolioFeedbackRepository.findAverageRatingByOwnerId(owner.getId());
        response.setPortfolioRating(avg == null ? 0D : roundAverage(avg));
        return response;
    }

    @Transactional
    public FeedbackSubmissionResponse submitPublicPlatformFeedback(
            String username,
            FeedbackSubmitRequest request,
            String visitorToken
    ) {
        User owner = findUserByUsername(username);
        PlatformFeedback ratingFeedback = null;
        PlatformFeedback messageFeedback = null;
        boolean ratingUpdatedExisting = false;

        if (request.getRating() != null) {
            String tokenHash = visitorTokenHash(visitorToken);
            ratingFeedback = platformFeedbackRepository
                    .findByPortfolioOwnerIdAndVisitorTokenHashAndSourceType(owner.getId(), tokenHash, PUBLIC_VISITOR_SOURCE)
                    .orElseGet(() -> PlatformFeedback.builder()
                            .portfolioOwner(owner)
                            .sourceType(PUBLIC_VISITOR_SOURCE)
                            .visitorTokenHash(tokenHash)
                            .build());
            ratingUpdatedExisting = ratingFeedback.getId() != null;
            ratingFeedback.setRating(request.getRating());
            ratingFeedback = platformFeedbackRepository.save(ratingFeedback);
        }

        if (hasText(request.getMessage())) {
            messageFeedback = platformFeedbackRepository.save(
                    PlatformFeedback.builder()
                            .portfolioOwner(owner)
                            .sourceType(PUBLIC_VISITOR_SOURCE)
                            .submitterName(cleanName(request.getName()))
                            .message(clean(request.getMessage()))
                            .build()
            );
            feedbackEmailService.sendPlatformFeedbackNotification("public visitor on @" + owner.getUsername(), messageFeedback);
        } else if (ratingFeedback != null && !ratingUpdatedExisting) {
            feedbackEmailService.sendPlatformFeedbackNotification("public visitor on @" + owner.getUsername(), ratingFeedback);
        }

        PlatformFeedback responseTarget = messageFeedback != null ? messageFeedback : ratingFeedback;
        if (responseTarget == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least a rating or a message");
        }

        return toSubmissionResponse(responseTarget, messageFeedback == null && ratingUpdatedExisting);
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
    public Double getPublicPortfolioAverageRating(String username) {
        User owner = findUserByUsername(username);
        Double averageRating = portfolioFeedbackRepository.findAverageRatingByOwnerId(owner.getId());
        return averageRating == null ? 0D : roundAverage(averageRating);
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
        PlatformFeedback ratingFeedback = null;
        PlatformFeedback messageFeedback = null;
        boolean ratingUpdatedExisting = false;

        if (request.getRating() != null) {
            ratingFeedback = platformFeedbackRepository.findBySubmittedByUserId(user.getId())
                    .orElseGet(() -> PlatformFeedback.builder()
                            .portfolioOwner(user)
                            .submittedByUser(user)
                            .sourceType(PORTFOLIO_OWNER_SOURCE)
                            .build());
            ratingUpdatedExisting = ratingFeedback.getId() != null;
            ratingFeedback.setRating(request.getRating());
            ratingFeedback = platformFeedbackRepository.save(ratingFeedback);
        }

        if (hasText(request.getMessage())) {
            messageFeedback = platformFeedbackRepository.save(
                    PlatformFeedback.builder()
                            .portfolioOwner(user)
                            .sourceType(PORTFOLIO_OWNER_SOURCE)
                            .submitterName(cleanName(request.getName()))
                            .message(clean(request.getMessage()))
                            .build()
            );
            feedbackEmailService.sendPlatformFeedbackNotification("portfolio owner @" + user.getUsername(), messageFeedback);
        } else if (ratingFeedback != null && !ratingUpdatedExisting) {
            feedbackEmailService.sendPlatformFeedbackNotification("portfolio owner @" + user.getUsername(), ratingFeedback);
        }

        PlatformFeedback responseTarget = messageFeedback != null ? messageFeedback : ratingFeedback;
        if (responseTarget == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide at least a rating or a message");
        }

        return toSubmissionResponse(responseTarget, messageFeedback == null && ratingUpdatedExisting);
    }

    private ReceivedFeedbackDto toReceivedDto(PortfolioFeedback feedback) {
        return ReceivedFeedbackDto.builder()
                .id(feedback.getId())
                .submitterName(displayName(feedback.getSubmitterName()))
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

    private String visitorTokenHash(String visitorToken) {
        if (!hasText(visitorToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Visitor token is required");
        }
        return hashIdentifier("visitor", clean(visitorToken));
    }

    private double roundAverage(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private String cleanName(String value) {
        String cleaned = clean(value);
        return hasText(cleaned) ? cleaned : null;
    }

    private String displayName(String value) {
        return hasText(value) ? clean(value) : "Anonymous";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private FeedbackSubmissionResponse toSubmissionResponse(PortfolioFeedback feedback, boolean updatedExisting) {
        return FeedbackSubmissionResponse.builder()
                .id(feedback.getId())
                .rating(feedback.getRating())
                .message(feedback.getMessage())
                .updatedExisting(updatedExisting)
                .messageStored(hasText(feedback.getMessage()))
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    private FeedbackSubmissionResponse toSubmissionResponse(PlatformFeedback feedback, boolean updatedExisting) {
        return FeedbackSubmissionResponse.builder()
                .id(feedback.getId())
                .rating(feedback.getRating())
                .message(feedback.getMessage())
                .updatedExisting(updatedExisting)
                .messageStored(hasText(feedback.getMessage()))
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
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
