package com.srijan.portfolio.service.ai;

import com.srijan.portfolio.config.AiProviderConfig;
import com.srijan.portfolio.dto.ResumeParseResponseDto;
import com.srijan.portfolio.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class AiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestratorService.class);

    private final List<AiProvider> aiProviders;
    private final AiProviderConfig config;
    @Qualifier("aiOrchestratorExecutor")
    private final ExecutorService aiOrchestratorExecutor;

    public ResumeParseResponseDto parseResume(
            byte[] fileBytes,
            String fileType,
            String extractedText,
            Predicate<ResumeParseResponseDto> validator) {
        return runWithFallback(provider -> provider.parseResume(fileBytes, fileType, extractedText), validator);
    }

    public String generateJson(String systemPrompt, String userPrompt, Predicate<String> validator) {
        return runWithFallback(provider -> provider.generateJson(systemPrompt, userPrompt), validator);
    }

    public String generateText(String systemPrompt, String userPrompt, Predicate<String> validator) {
        return runWithFallback(provider -> provider.generateText(systemPrompt, userPrompt), validator);
    }

    private <T> T runWithFallback(ProviderCallback<T> callback, Predicate<T> validator) {
        List<AiProvider> orderedProviders = orderProviders();
        List<String> failures = new ArrayList<>();

        for (AiProvider provider : orderedProviders) {
            if (!provider.isAvailable()) {
                log.info("ai.provider.skipped provider={} reason=not_available", provider.getName());
                continue;
            }

            for (int attempt = 1; attempt <= config.getMaxRetryAttempts(); attempt++) {
                if (attempt > 1 && !backoffBeforeRetry(provider, attempt)) {
                    break;
                }
                try {
                    T result = executeWithTimeout(provider, () -> callback.apply(provider));
                    if (result == null || !validator.test(result)) {
                        String reason = "invalid_response";
                        failures.add(provider.getName() + ":" + reason);
                        log.warn("ai.provider.invalid provider={} attempt={} reason={}", provider.getName(), attempt, reason);
                        continue;
                    }

                    log.info("ai.provider.success provider={} attempt={}", provider.getName(), attempt);
                    return result;
                } catch (TimeoutException exception) {
                    failures.add(provider.getName() + ":timeout");
                    log.warn("ai.provider.failure provider={} attempt={} reason=timeout timeoutMs={}",
                            provider.getName(), attempt, config.getTimeoutMs());
                } catch (Exception exception) {
                    failures.add(provider.getName() + ":" + exception.getClass().getSimpleName());
                    log.warn("ai.provider.failure provider={} attempt={} reason={}",
                            provider.getName(), attempt, sanitizeFailure(exception));
                }
            }
        }

        throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "AI_PROVIDER_FAILURE",
                "All AI providers failed. Please try again shortly."
        );
    }

    /**
     * Waits before re-attempting the same provider, scaled by attempt number.
     *
     * @return false if the thread was interrupted while waiting, in which case the caller must stop
     *         retrying rather than spin through the remaining attempts.
     */
    private boolean backoffBeforeRetry(AiProvider provider, int attempt) {
        long delayMs = config.getRetryBackoffMs() * (attempt - 1);
        if (delayMs <= 0) {
            return true;
        }
        try {
            log.debug("ai.provider.backoff provider={} nextAttempt={} delayMs={}",
                    provider.getName(), attempt, delayMs);
            Thread.sleep(delayMs);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private List<AiProvider> orderProviders() {
        List<String> priority = config.getProviderPriority();
        return aiProviders.stream()
                .sorted(Comparator.comparingInt(provider -> {
                    int index = priority.indexOf(provider.getName().toLowerCase());
                    return index >= 0 ? index : Integer.MAX_VALUE;
                }))
                .toList();
    }

    private <T> T executeWithTimeout(AiProvider provider, Callable<T> callable) throws Exception {
        Future<T> future = aiOrchestratorExecutor.submit(callable);
        try {
            return future.get(config.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw exception;
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception inner) {
                throw inner;
            }
            throw new IllegalStateException("Unexpected AI provider failure", cause);
        }
    }

    private String sanitizeFailure(Exception exception) {
        String message = Objects.toString(exception.getMessage(), exception.getClass().getSimpleName());
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    @FunctionalInterface
    private interface ProviderCallback<T> {
        T apply(AiProvider provider) throws Exception;
    }
}
