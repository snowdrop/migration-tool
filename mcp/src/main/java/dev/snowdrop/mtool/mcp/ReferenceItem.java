package dev.snowdrop.mtool.mcp;

/**
 * One row of a Spring to Quarkus reference table (annotation, config or dependency map).
 */
public record ReferenceItem(String section, String spring, String quarkus, Compat compat, String notes) {

    /**
     * Support level of the Spring construct.
     */
    public enum Compat {
        SUPPORTED,
        NOT_SUPPORTED,
        SUPPORTED_WITH_CHANGE
    }
}
