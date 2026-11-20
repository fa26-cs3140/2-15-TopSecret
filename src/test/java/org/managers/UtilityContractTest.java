package org.managers;

import org.controller.DebugHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.model.SearchResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtilityContractTest {

    @TempDir
    Path directory;

    @Test
    void changingFileDirectoryChangesWhereFilesAreWritten() throws Exception {
        FileHandler files = new FileHandler(directory.toString());
        assertEquals(directory.toString(), files.getDataDirectory());
        Path other = Files.createDirectory(directory.resolve("other"));
        files.setDataDirectory(other.toString());

        assertTrue(files.writeFile("report.txt", "Moved directory"));
        assertEquals("Moved directory", files.readFile("report.txt"));
        assertTrue(Files.exists(other.resolve("report.txt")));
        assertFalse(Files.exists(directory.resolve("report.txt")));
    }

    @Test
    void searchResultPreservesMatchesAndStatus() {
        SearchResult result = new SearchResult(List.of("Brief 7"), "One result");
        assertEquals(List.of("Brief 7"), result.getMatches());
        assertEquals("One result", result.getMessage());
    }

    @Test
    void diagnosticsPrintArrayIndexesAndValues() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        try {
            System.setOut(new PrintStream(output));
            assertNotNull(new DebugHelper());
            DebugHelper.debugPrintArray(new Object[]{"first", "second"});
        } finally {
            System.setOut(previous);
        }
        assertTrue(output.toString().contains("0 : first"));
        assertTrue(output.toString().contains("1 : second"));
    }

    @Test
    void missionImportIsExplicitlyUnsupported() throws Exception {
        MissionManager missions = new MissionManager("jdbc:sqlite:" + directory.resolve("unused.db"));
        assertEquals("jdbc:sqlite:" + directory.resolve("unused.db"), missions.getURL());
        assertEquals(0, missions.importTSV("missing.tsv", "missions", directory.toString(), "unused.db"));
        assertFalse(Files.exists(directory.resolve("unused.db")));
    }
}
