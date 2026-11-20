package org.managers;

import org.model.Agent;
import org.model.Brief;
import org.model.Facility;
import org.model.RecordType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Parses and validates a complete TSV before any imported records are written. */
public final class RecordImport {
    public List<Object> read(RecordType type, Path path) throws IOException {
        if (type == RecordType.COUNTRY || type == RecordType.MISSION) {
            throw new IllegalArgumentException("This content type does not support TSV imports.");
        }
        List<Object> records = new ArrayList<>();
        try (var reader = Files.newBufferedReader(path)) {
            reader.readLine(); // All existing TSV formats include a header.
            String line;
            int number = 1;
            while ((line = reader.readLine()) != null) {
                number++;
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = (type == RecordType.FACILITY ? line.replace("\\t", "\t") : line)
                        .split("\t", -1);
                try {
                    records.add(parse(type, fields));
                } catch (IllegalArgumentException | IndexOutOfBoundsException invalid) {
                    throw new IllegalArgumentException("Invalid TSV row " + number + ": " + invalid.getMessage(), invalid);
                }
            }
        }
        return List.copyOf(records);
    }

    private static Object parse(RecordType type, String[] fields) {
        return switch (type) {
            case BRIEF -> new Brief(1, fields[0], fields[1], fields[2], null);
            case AGENT -> new Agent(1, fields[0], fields[1],
                    fields[2].isBlank() ? null : fields[2], fields[3].isBlank() ? null : fields[3],
                    Integer.parseInt(fields[4].trim()));
            case FACILITY -> fields.length > 2 && !fields[2].isBlank()
                    ? new Facility(1, fields[0], fields[1], Integer.parseInt(fields[2].trim()))
                    : new Facility(1, fields[0], fields[1]);
            default -> throw new IllegalArgumentException("Unsupported TSV content type.");
        };
    }
}
