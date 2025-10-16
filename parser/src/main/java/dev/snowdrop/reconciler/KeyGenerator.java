package dev.snowdrop.reconciler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates padded keys in the format: <ID>-ddd where:
 * - ID is a prefix string (typically a rule ID)
 * - ddd is a 3-digit zero-padded sequence number (001-999)
 *
 * Example: "RECIPE-001", "QUERY-042", "R123-999"
 */
public class KeyGenerator {
    private static final KeyGenerator INSTANCE = new KeyGenerator();

    private final Map<String, AtomicInteger> countersByRuleId = new ConcurrentHashMap<>();

    /**
     * Generate a key in format: <ruleId>-ddd (e.g., "RULE123-001")
     *
     * @param ruleId The rule/recipe identifier prefix
     * @return Formatted key with 3-digit padding
     * @throws IllegalStateException if more than 999 records are generated for the same ruleId
     */
    public String generateKey(String ruleId) {
        AtomicInteger counter = countersByRuleId.computeIfAbsent(
            ruleId,
            k -> new AtomicInteger(0)
        );

        int sequence = counter.incrementAndGet();
        if (sequence > 999) {
            throw new IllegalStateException(
                "Maximum 999 records exceeded for rule: " + ruleId
            );
        }

        return String.format("%s-%03d", ruleId, sequence);
    }

    /**
     * Get the current count for a specific rule ID without incrementing
     *
     * @param ruleId The rule identifier
     * @return Current count, or 0 if no keys have been generated yet
     */
    public int getCurrentCount(String ruleId) {
        AtomicInteger counter = countersByRuleId.get(ruleId);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Reset counter for a specific rule ID
     *
     * @param ruleId The rule identifier to reset
     */
    public void reset(String ruleId) {
        countersByRuleId.remove(ruleId);
    }

    /**
     * Reset all counters
     */
    public void resetAll() {
        countersByRuleId.clear();
    }

    // Static convenience methods using singleton instance

    /**
     * Static method to generate a key using the shared singleton instance
     *
     * @param ruleId The rule/recipe identifier prefix
     * @return Formatted key with 3-digit padding
     */
    public static String generate(String ruleId) {
        return INSTANCE.generateKey(ruleId);
    }

    /**
     * Static method to get current count using the shared singleton instance
     *
     * @param ruleId The rule identifier
     * @return Current count, or 0 if no keys have been generated yet
     */
    public static int getCount(String ruleId) {
        return INSTANCE.getCurrentCount(ruleId);
    }

    /**
     * Static method to reset counter for a specific rule ID
     *
     * @param ruleId The rule identifier to reset
     */
    public static void resetCounter(String ruleId) {
        INSTANCE.reset(ruleId);
    }

    /**
     * Static method to reset all counters
     */
    public static void resetAllCounters() {
        INSTANCE.resetAll();
    }
}