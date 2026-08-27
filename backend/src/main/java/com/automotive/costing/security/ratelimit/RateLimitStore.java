package com.automotive.costing.security.ratelimit;

/**
 * Pluggable fixed-window counter for API rate limiting.
 * Implementations: in-memory (single node) or Redis (multi-instance).
 */
public interface RateLimitStore {

    /**
     * @return true if the request is within limit and was recorded
     */
    boolean tryAcquire(String key, int limit, int windowSeconds);
}
