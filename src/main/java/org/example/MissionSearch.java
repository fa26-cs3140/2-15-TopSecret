package org.example;

import java.util.Arrays;
import java.util.List;

// TODO: Add Javadoc
public class MissionSearch {
    private DatabaseManager databaseManager;

    public MissionSearch() {
        this.databaseManager = new DatabaseManager();
    }

    public MissionSearch(DatabaseManager databaseManager) {
        // NOTE: Maybe this constructor isn't necessary anymore
        this.databaseManager = databaseManager;
    }

    public SearchResult search(String searchTerm) {
        // Returns empty list if searchTerm is blank or null
        if (searchTerm == null || searchTerm.isBlank()) {
            return new SearchResult(List.of(), "Search term cannot be blank");
        }

        String searchKey = "%" + searchTerm + "%";
        String[] records = databaseManager.getRecords("Text", searchKey);

        // Search operation fails
        if (records == null) {
            return new SearchResult(null, "Search failed.");
        }

        // Search operation completed but no matching records found
        if (records.length == 0) {
            return new SearchResult(List.of(), "No matches were found.");
        }

        // Matches found
        List<String> matches = Arrays.asList(records);

        return new SearchResult(matches, null);
    }
}
