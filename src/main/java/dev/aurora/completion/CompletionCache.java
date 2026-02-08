package dev.aurora.completion;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache for expensive tab completion results.
 * Automatically expires entries after a TTL period.
 */
public class CompletionCache {
    private final Map<String, CachedCompletion> cache;
    private static final long DEFAULT_TTL = 5000; // 5 seconds

    public CompletionCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Get completions from cache or compute if missing/expired.
     *
     * @param key The cache key
     * @param loader The supplier to compute completions if not cached
     * @return The completion list
     */
    public List<String> get(String key, Supplier<List<String>> loader) {
        return get(key, loader, DEFAULT_TTL);
    }

    /**
     * Get completions from cache or compute if missing/expired.
     *
     * @param key The cache key
     * @param loader The supplier to compute completions if not cached
     * @param ttlMillis Time-to-live in milliseconds
     * @return The completion list
     */
    public List<String> get(String key, Supplier<List<String>> loader, long ttlMillis) {
        CachedCompletion cached = cache.get(key);

        // Check if cached and not expired
        if (cached != null && !cached.isExpired()) {
            return cached.values;
        }

        // Compute new value
        List<String> newValues = loader.get();
        cache.put(key, new CachedCompletion(newValues, System.currentTimeMillis() + ttlMillis));

        return newValues;
    }

    /**
     * Invalidate a cache entry.
     *
     * @param key The cache key to invalidate
     */
    public void invalidate(String key) {
        cache.remove(key);
    }

    /**
     * Clear all cached entries.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Clean up expired entries.
     * Should be called periodically.
     */
    public void cleanupExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Get the number of cached entries.
     *
     * @return Cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Cached completion with expiration.
     */
    private static class CachedCompletion {
        final List<String> values;
        final long expiresAt;

        CachedCompletion(List<String> values, long expiresAt) {
            this.values = values;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
