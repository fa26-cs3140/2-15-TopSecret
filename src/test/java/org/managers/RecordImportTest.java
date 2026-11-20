package org.managers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.model.Brief;
import org.model.RecordType;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RecordImportTest {
    @TempDir
    Path directory;

    @Test
    void validatesTheWholeFileAndReportsTheInvalidRow() throws Exception {
        Path file = directory.resolve("briefs.tsv");
        Files.writeString(file, "title\tdate\ttext\nFirst\t2020-01-01\ttext\nIncomplete\n");
        var failure = assertThrows(IllegalArgumentException.class,
                () -> new RecordImport().read(RecordType.BRIEF, file));
        assertTrue(failure.getMessage().contains("row 3"));
        Files.writeString(file, "title\tdate\ttext\nO'Brien\t2020-01-01\ttext\n");
        var parsed = new RecordImport().read(RecordType.BRIEF, file);
        assertEquals("O'Brien", ((Brief) parsed.getFirst()).getTitle());
        assertThrows(UnsupportedOperationException.class, () -> parsed.clear());
    }

    @Test
    void handlesEmptyFilesAndRejectsUnsupportedOrMissingInputs() throws Exception {
        Path empty = directory.resolve("empty.tsv");
        Files.writeString(empty, "");
        var importer = new RecordImport();
        assertTrue(importer.read(RecordType.BRIEF, empty).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> importer.read(RecordType.COUNTRY, empty));
        assertThrows(IllegalArgumentException.class, () -> importer.read(RecordType.MISSION, empty));
        assertThrows(java.io.IOException.class, () -> importer.read(RecordType.AGENT, directory.resolve("missing")));
    }
}
