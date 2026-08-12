package com.srijan.portfolio.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory fixed-window counter store shared by the rate limiting filters.
 *
 * <p>Counters are evicted periodically and the key space is hard-capped, so a caller cannot grow
 * the map without bound. State is per instance: with more than one running instance the effective
 * limit is multiplied by the instance count, which is acceptable for the current single-instance
 * deployment but is the reason to move to a shared store (Redis) if that ever changes.
 */
public class FixedWindowRateLimiter {

    /** Outcome of a single counted request. */
    public record Hit(int limit, int remaining, long resetEpochSecond, boolean exceeded) {

        public long retryAfterSeconds() {
            return Math.max(resetEpochSecond - Instant.now().getEpochSecond(), 1);
        }
    }

    private final Duration window;
    private final int maxTrackedKeys;
    private final int evictionInterval;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requestsSinceEviction = new AtomicLong();

    public FixedWindowRateLimiter(Duration window, int maxTrackedKeys, int evictionInterval) {
        this.window = window;
        this.maxTrackedKeys = maxTrackedKeys;
        this.evictionInterval = evictionInterval;
    }

    /**
     * Counts one request against {@code key} and reports the resulting quota state.
     *
     * <p>Fails open (never blocks) if the key space is saturated, so a tracking failure degrades
     * into "no limit" rather than into a hard outage.
     */
    public Hit hit(String key, int limit) {
        maybeEvict();

        if (counters.size() >= maxTrackedKeys && !counters.containsKey(key)) {
            return new Hit(limit, limit, Instant.now().plus(window).getEpochSecond(), false);
        }

        WindowCounter counter = counters.compute(key, (ignored, existing) ->
                existing == null || existing.isExpired(window) ? new WindowCounter() : existing);

        int used = counter.incrementAndGet();
        return new Hit(
                limit,
                Math.max(limit - used, 0),
                counter.resetEpochSecond(window),
                used > limit
        );
    }

    /** Visible for diagnostics — the number of live counters currently held. */
    public int trackedKeys() {
        return counters.size();
    }

    private void maybeEvict() {
        boolean saturated = counters.size() >= maxTrackedKeys;
        if (requestsSinceEviction.incrementAndGet() < evictionInterval && !saturated) {
            return;
        }
        requestsSinceEviction.set(0);
        counters.values().removeIf(counter -> counter.isExpired(window));
    }

    private static final class WindowCounter {

        private final Instant startedAt = Instant.now();
        private final AtomicInteger count = new AtomicInteger();

        boolean isExpired(Duration window) {
            return startedAt.plus(window).isBefore(Instant.now());
        }

        long resetEpochSecond(Duration window) {
            return startedAt.plus(window).getEpochSecond();
        }

        int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
