import org.example.FileHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// For file handling
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// FileHandler class

public class FileHandlerTest {
    private Path testDirectory;
    private FileHandler fileHandler;

    @BeforeEach
    void setUp() throws IOException {
        // Create a testData directory to read from
        testDirectory = Path.of("testData");
        Files.createDirectory(testDirectory);

        String file1Content = "This is file 1";
        Files.writeString(testDirectory.resolve("file1.txt"), file1Content);

        String file2Content = "This is file 2";
        Files.writeString(testDirectory.resolve("file2.txt"), file2Content);

        String multilineContent = "Line 1\nLine 2\nLine 3";
        Files.writeString(testDirectory.resolve("multiline.txt"), multilineContent);

        Files.writeString(testDirectory.resolve("empty.txt"), "");

        fileHandler = new FileHandler("testData");
    }

    @AfterEach
    void clear() throws IOException {
        // Delete the testData directory
        File[] allFiles = testDirectory.toFile().listFiles();

        if (allFiles != null) {
            for (File file : allFiles)
                Files.delete(file.toPath());
        }

        Files.delete(testDirectory);
    }

    @Test
    void getAvailableFilesReturnsAllFiles() {
        List<String> files = fileHandler.getAvailableFiles();

        assertEquals(4, files.size());
        assertTrue(files.contains("file1.txt"));
        assertTrue(files.contains("file2.txt"));
        assertTrue(files.contains("multiline.txt"));
        assertTrue(files.contains("empty.txt"));
    }

    @Test
    void readFileReturnsCorrectContents() {
        String contents = fileHandler.readFile("file1.txt");

        assertEquals("This is file 1.", contents);
    }

    @Test
    void readFileReturnsMultilineContents() {
        String contents = fileHandler.readFile("multiline.txt");

        assertEquals("Line 1\nLine 2\nLine 3", contents);
    }

    @Test
    void readFileReturnsEmptyStringForEmptyFile() {
        String contents = fileHandler.readFile("empty.txt");

        assertEquals("", contents);
    }

    @Test
    void readFileDoesNotCrashForMissingFile() {
        assertDoesNotThrow(() ->
                fileHandler.readFile("doesNotExist.txt")
        );
    }
}
