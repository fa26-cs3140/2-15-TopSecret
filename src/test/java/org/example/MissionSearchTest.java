package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MissionSearchTest {

    @Mock
    private DatabaseManager databaseManager;

    private MissionSearch missionSearch;

    @BeforeEach
    void setup() {
        missionSearch = new MissionSearch(databaseManager);
    }

    @Test
    void searchUsingTextColumn() {
        String record = "Operation Sandtrap\t1970-11-03\tA text including the word agent in it.";

        when(databaseManager.getRecords("Text", "%agent%")).thenReturn(new String[]{record}); // databaseManager returns a list of Strings including the given record

        SearchResult result = missionSearch.search("agent");

        assertEquals(List.of(record), result.getMatches());
        assertNull(result.getMessage());
    }

    @Test
    void searchReturnsMultipleMatches() {
        String record1 = "Operation Sandtrap\t1970-11-03\tA text including the word agent in it.";
        String record2 = "Project Bluebird\t1973-04-20\tAnother text including the word agent in it.";

        when(databaseManager.getRecords("Text", "%agent%")).thenReturn(new String[]{record1, record2});

        SearchResult result = missionSearch.search("agent");

        assertEquals(2, result.getMatches().size());
        assertEquals(record1, result.getMatches().get(0));
        assertEquals(record2, result.getMatches().get(1));
    }

    @Test
    void searchReturnsNoMatchesMessage() {
        when(databaseManager.getRecords("Text", "%missing%")).thenReturn(new String[0]);

        SearchResult result = missionSearch.search("missing");

        assertTrue(result.getMatches().isEmpty());
        assertEquals("No matches were found.", result.getMessage());
    }

    @Test
    void searchDatabaseFailed() {
        when(databaseManager.getRecords("Text", "%agent%")).thenReturn(null);

        SearchResult result = missionSearch.search("agent");

        assertNull(result.getMatches());
        assertEquals("Search failed.", result.getMessage());
    }

    @Test
    void nullSearchReturnsEmpty() {
        SearchResult result = missionSearch.search(null);

        assertTrue(result.getMatches().isEmpty());
        assertNotNull(result.getMessage());
    }

    @Test
    void blankSearchReturnsEmpty() {
        SearchResult result = missionSearch.search("              ");

        assertTrue(result.getMatches().isEmpty());
        assertNotNull(result.getMessage());
    }
}
