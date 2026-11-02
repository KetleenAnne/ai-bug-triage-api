package dev.ketleen.triage.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A bug report as submitted by a user. Treated as untrusted input. */
public record TriageRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String description,
        @Size(max = 200) String environment) {
}
