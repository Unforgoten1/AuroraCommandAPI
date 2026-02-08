package dev.aurora.ratelimit;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Implements a sliding window rate limiting algorithm.
 * Efficiently tracks request timestamps and enforces limits.
 */
public class SlidingWindow {
    private final Queue<Long> timestamps;
    private final int maxRequests;
    private final long windowMillis;

    /**
     * Create a sliding window rate limiter.
     *
     * @param maxRequests Maximum number of requests allowed
     * @param windowMillis Time window in milliseconds
     */
    public SlidingWindow(int maxRequests, long windowMillis) {
        this.timestamps = new LinkedList<>();
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /**
     * Try to acquire a permit (execute a request).
     * Returns true if the request is allowed, false if rate limited.
     *
     * @return True if allowed, false if rate limited
     */
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        cleanupOldTimestamps(now);

        if (timestamps.size() < maxRequests) {
            timestamps.offer(now);
            return true;
        }

        return false;
    }

    /**
     * Check if a request would be allowed without consuming a permit.
     *
     * @return True if a request would be allowed
     */
    public synchronized boolean wouldAllow() {
        long now = System.currentTimeMillis();
        cleanupOldTimestamps(now);
        return timestamps.size() < maxRequests;
    }

    /**
     * Get the number of requests in the current window.
     *
     * @return Number of requests
     */
    public synchronized int getRequestCount() {
        cleanupOldTimestamps(System.currentTimeMillis());
        return timestamps.size();
    }

    /**
     * Get the remaining number of requests allowed.
     *
     * @return Remaining requests
     */
    public synchronized int getRemaining() {
        cleanupOldTimestamps(System.currentTimeMillis());
        return Math.max(0, maxRequests - timestamps.size());
    }

    /**
     * Get the time (in milliseconds) until the next request will be allowed.
     * Returns 0 if a request is currently allowed.
     *
     * @return Time until next request in milliseconds
     */
    public synchronized long getTimeUntilNextRequest() {
        cleanupOldTimestamps(System.currentTimeMillis());

        if (timestamps.size() < maxRequests) {
            return 0;
        }

        // Next request allowed when oldest timestamp expires
        Long oldest = timestamps.peek();
        if (oldest == null) {
            return 0;
        }

        long expiresAt = oldest + windowMillis;
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    /**
     * Reset the rate limiter, clearing all timestamps.
     */
    public synchronized void reset() {
        timestamps.clear();
    }

    /**
     * Remove timestamps that are outside the current window.
     *
     * @param now Current time in milliseconds
     */
    private void cleanupOldTimestamps(long now) {
        long cutoff = now - windowMillis;
        while (!timestamps.isEmpty() && timestamps.peek() < cutoff) {
            timestamps.poll();
        }
    }

    /**
     * Get the maximum requests allowed.
     *
     * @return Max requests
     */
    public int getMaxRequests() {
        return maxRequests;
    }

    /**
     * Get the window duration in milliseconds.
     *
     * @return Window duration
     */
    public long getWindowMillis() {
        return windowMillis;
    }
}
