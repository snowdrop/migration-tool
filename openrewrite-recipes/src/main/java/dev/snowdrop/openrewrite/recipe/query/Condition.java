package dev.snowdrop.openrewrite.recipe.query;

import java.util.Map;

// The base interface for all conditions
public interface Condition {
    boolean evaluate(Map<String, String> properties);
}
    record SimpleCondition(String key, String operator, String value) implements Condition {
        public boolean evaluate(Map<String, String> properties) {
            String propertyValue = properties.get(key);
            if (propertyValue == null) return false;

            return "MATCHES".equals(operator)
                ? propertyValue.matches(value)
                : propertyValue.equals(value);
        }
    }

    // Represents two conditions joined by AND
    record AndCondition(Condition left, Condition right) implements Condition {
        public boolean evaluate(Map<String, String> properties) {
            return left.evaluate(properties) && right.evaluate(properties);
        }
    }

    // Represents two conditions joined by OR
    record OrCondition(Condition left, Condition right) implements Condition {
        public boolean evaluate(Map<String, String> properties) {
            return left.evaluate(properties) || right.evaluate(properties);
        }
    }
