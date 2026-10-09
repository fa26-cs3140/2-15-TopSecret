package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class MissionSearchIntegrationTest {
    private Path testDirectory;
    private final String testDirName = "testDir";
    private final String testDatabase = "testDatabase.db";
    private final String missionData = "data/mission_briefs.tsv";
    private DatabaseManager testManager;

    private final String testURL = "jdbc:sqlite:" + testDirName + "/" + testDatabase;

    private MissionSearch missionSearch;

    @BeforeEach
    void setup() throws IOException {
        this.testDirectory = Path.of(testDirName);
        Files.createDirectory(testDirectory);

        this.testManager = new DatabaseManager(testURL);
        this.createTestDatabase(this.testManager);

        this.missionSearch = new MissionSearch(this.testManager);
    }

    @AfterEach
    void cleanUp() throws IOException
    {
        File[] allFiles = testDirectory.toFile().listFiles();

        if (allFiles != null)
        {
            for (File file : allFiles)
                Files.delete(file.toPath());
        }

        Files.delete(testDirectory);
    }

    @Test
    void searchFindsMatchingMissionBriefs() {
        SearchResult result = missionSearch.search("diplomatic pouch");

        assertNotNull(result);
        assertNotNull(result.getMatches());

        assertEquals(1, result.getMatches().size());
        assertTrue(result.getMatches().getFirst().startsWith("Operation Sandtrap"));

        assertNull(result.getMessage());
    }

    @Test
    void searchIsCaseInsensitive() {
        SearchResult result = missionSearch.search("diPLOmatIc pOUch");

        assertNotNull(result);
        assertNotNull(result.getMatches());

        System.out.println("Matches: " + result.getMatches());

        assertEquals(1, result.getMatches().size());
        assertTrue(result.getMatches().getFirst().startsWith("Operation Sandtrap"));

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

        assertEquals("No matches were found.", result.getMessage());
    }

    private void createTestDatabase(DatabaseManager manager)
    {
        // Create temporary database
        try
        {
            manager.importTSV(this.missionData,
                    this.testDirectory.toString(), this.testDatabase);
        }
        catch (IOException e)
        {
            System.err.print(e.getMessage());
        }
    }
}
