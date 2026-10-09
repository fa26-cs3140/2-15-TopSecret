package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MissionSearchIntegrationTest {
    private MissionSearch missionSearch;

    @BeforeEach
    void setup() {
        DatabaseManager databaseManager = new DatabaseManager();
        missionSearch = new MissionSearch(databaseManager);
    }

    @Test
    void searchFindsMatchingMissionBriefs() {
        SearchResult result = missionSearch.search("diplomatic pouch");

        assertNotNull(result);
        assertNotNull(result.getMatches());

        assertEquals(1, result.getMatches().size());
        assertTrue(result.getMatches().contains("Operation Sandtrap"));

        assertNull(result.getMessage());
    }

    @Test
    void searchIsCaseInsensitive() {
        SearchResult result = missionSearch.search("diPLOmatIc pOUch");

        assertNotNull(result);
        assertNotNull(result.getMatches());

        assertEquals(1, result.getMatches().size());
        assertTrue(result.getMatches().contains("Operation Sandtrap"));

        assertNull(result.getMessage());
    }

    @Test
    void searchDoesNotSearchTitles() {
        // There is no mission text including "Operation Sandtrap"
        SearchResult result = missionSearch.search("Operation Sandtrap");

        assertNotNull(result);
        assertNotNull(result.getMatches());

        assertTrue(result.getMatches().isEmpty());

        assertEquals("No matches were found.", result.getMessage());
    }

    @Test
    void searchReturnsNoMatchesMessage() {
        SearchResult result = missionSearch.search("mike ly"); // no way my name is in there

        assertNotNull(result);
        assertNotNull(result.getMatches());

        assertTrue(result.getMatches().isEmpty());

        assertEquals("No matches found.", result.getMessage());
    }
}
