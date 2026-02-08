package dev.aurora.ratelimit;

/**
 * Configuration for command rate limiting.
 * Supports per-player, per-IP, and global rate limits.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * RateLimitConfig config = RateLimitConfig.create()
 *     .perPlayer(1, 86400)  // 1 per day per player
 *     .perIP(5, 86400)      // Max 5 per day per IP
 *     .burst(2);            // Allow 2 burst requests
 * }</pre>
 */
public class RateLimitConfig {
    private int perPlayerRequests = 0;
    private long perPlayerWindowSeconds = 0;

    private int perIPRequests = 0;
    private long perIPWindowSeconds = 0;

    private int globalRequests = 0;
    private long globalWindowSeconds = 0;

    private int burstAllowance = 0;
    private String rateLimitMessage = "§cRate limit exceeded! Please try again later.";

    private RateLimitConfig() {
    }

    /**
     * Create a new rate limit configuration.
     *
     * @return New config builder
     */
    public static RateLimitConfig create() {
        return new RateLimitConfig();
    }

    /**
     * Set per-player rate limit.
     *
     * @param requests Maximum requests per window
     * @param windowSeconds Time window in seconds
     * @return This config for chaining
     */
    public RateLimitConfig perPlayer(int requests, long windowSeconds) {
        this.perPlayerRequests = requests;
        this.perPlayerWindowSeconds = windowSeconds;
        return this;
    }

    /**
     * Set per-IP rate limit.
     *
     * @param requests Maximum requests per window
     * @param windowSeconds Time window in seconds
     * @return This config for chaining
     */
    public RateLimitConfig perIP(int requests, long windowSeconds) {
        this.perIPRequests = requests;
        this.perIPWindowSeconds = windowSeconds;
        return this;
    }

    /**
     * Set global rate limit (for all players combined).
     *
     * @param requests Maximum requests per window
     * @param windowSeconds Time window in seconds
     * @return This config for chaining
     */
    public RateLimitConfig global(int requests, long windowSeconds) {
        this.globalRequests = requests;
        this.globalWindowSeconds = windowSeconds;
        return this;
    }

    /**
     * Set burst allowance.
     * Allows this many extra requests before rate limiting kicks in.
     *
     * @param burstAllowance Number of burst requests allowed
     * @return This config for chaining
     */
    public RateLimitConfig burst(int burstAllowance) {
        this.burstAllowance = burstAllowance;
        return this;
    }

    /**
     * Set custom rate limit message.
     *
     * @param message The message to send when rate limited
     * @return This config for chaining
     */
    public RateLimitConfig message(String message) {
        this.rateLimitMessage = message;
        return this;
    }

    // Getters

    public boolean hasPerPlayerLimit() {
        return perPlayerRequests > 0;
    }

    public int getPerPlayerRequests() {
        return perPlayerRequests + burstAllowance;
    }

    public long getPerPlayerWindowMillis() {
        return perPlayerWindowSeconds * 1000;
    }

    public boolean hasPerIPLimit() {
        return perIPRequests > 0;
    }

    public int getPerIPRequests() {
        return perIPRequests + burstAllowance;
    }

    public long getPerIPWindowMillis() {
        return perIPWindowSeconds * 1000;
    }

    public boolean hasGlobalLimit() {
        return globalRequests > 0;
    }

    public int getGlobalRequests() {
        return globalRequests + burstAllowance;
    }

    public long getGlobalWindowMillis() {
        return globalWindowSeconds * 1000;
    }

    public String getRateLimitMessage() {
        return rateLimitMessage;
    }

    public int getBurstAllowance() {
        return burstAllowance;
    }
}
