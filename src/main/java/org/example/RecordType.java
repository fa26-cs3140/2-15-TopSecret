package org.example;

public enum RecordType {

    BRIEF("Briefs", "brief"),
    AGENT("Agents", "agent"),
    FACILITY("Facilities", "facility");

    private final String title;
    private final String singular;

    RecordType(String title, String singular) {
        this.title = title;
        this.singular = singular;
    }

    public String getTitle() {
        return title;
    }

    public String getSingular() {
        return singular;
    }
}
