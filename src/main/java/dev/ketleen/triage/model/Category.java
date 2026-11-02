package dev.ketleen.triage.model;

/** Bug category. The owning team is derived from the category, never taken from the LLM. */
public enum Category {
    SECURITY("security"),
    DATA("data"),
    PERFORMANCE("platform"),
    INTEGRATION("integrations"),
    UI_UX("frontend"),
    FUNCTIONAL("backend"),
    OTHER("triage");

    private final String team;

    Category(String team) {
        this.team = team;
    }

    public String team() {
        return team;
    }
}
