package org.example;

import java.util.List;

// TODO: Add javadoc
public class SearchResult {
    private final List<String> matches;
    private final String message;

    public SearchResult(List<String> matches, String message) {
        this.matches = matches;
        this.message = message;
    }

    public List<String> getMatches() {
        return matches;
    }

    public String getMessage() {
        return message;
    }
}
