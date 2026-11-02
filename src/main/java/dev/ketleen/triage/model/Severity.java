package dev.ketleen.triage.model;

/** Declared from most to least severe, so {@code ordinal()} 0 is the worst. */
public enum Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}
