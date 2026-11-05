package org.managers;

import org.model.Mission;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Mission persistence using the shared branch's comma-separated ID-list format.
 * List columns are TEXT, not scalar foreign keys. Facility references remain enforced.
 */
public class MissionManager extends IDatabaseManager<Mission> {
    private final String tableName = "missions";
    private final String[] columnNames = {"id", "title", "briefIds", "agentIds", "facilityId"};

    public MissionManager(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Database URL cannot be blank.");
        this.url = url;
    }
    @Override public String getURL() { return url; }
    @Override public String getTableName() { return tableName; }
    @Override public String[] getColumnNames() { return columnNames.clone(); }

    private String column(String column) {
        if (!Arrays.asList(columnNames).contains(column)) throw new IllegalArgumentException("Unknown mission column.");
        return column;
    }
    private static int[] parseIds(String text) {
        if (text == null || text.isBlank()) return new int[0];
        try {
            int[] ids = Arrays.stream(text.split(",", -1)).map(String::trim).mapToInt(Integer::parseInt).toArray();
            if (Arrays.stream(ids).anyMatch(id -> id <= 0)) throw new IllegalArgumentException("IDs must be positive.");
            return ids;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid comma-separated ID list.", e); }
    }
    private static String formatIds(int[] ids) {
        if (ids == null) return "";
        if (Arrays.stream(ids).anyMatch(id -> id <= 0)) throw new IllegalArgumentException("IDs must be positive.");
        return IntStream.of(ids).mapToObj(Integer::toString).collect(Collectors.joining(","));
    }
    @Override public List<Mission> getRecords(String column, String key) {
        String sql = "SELECT * FROM missions WHERE " + column(column) + " LIKE ? ORDER BY id";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, key);
            List<Mission> missions = new ArrayList<>();
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) missions.add(new Mission(rs.getInt("id"), rs.getString("title"),
                        parseIds(rs.getString("briefIds")), parseIds(rs.getString("agentIds")), rs.getInt("facilityId")));
            }
            return missions;
        } catch (SQLException e) { throw new IllegalStateException("Unable to read missions.", e); }
    }
    @Override public Mission addRecord(Mission value) {
        if (value == null) throw new IllegalArgumentException("Mission required.");
        if (value.getTitle() == null || value.getTitle().isBlank()) throw new IllegalArgumentException("Title required.");
        String briefs = formatIds(value.getBriefIds());
        String agents = formatIds(value.getAgentIds());
        int id = insert("INSERT INTO missions(title,briefIds,agentIds,facilityId) VALUES(?,?,?,?)",
                value.getTitle(), briefs, agents, value.getFacilityId());
        return new Mission(id, value.getTitle(), parseIds(briefs), parseIds(agents), value.getFacilityId());
    }
    public Mission addRecord(String title, int[] briefIds, int[] agentIds, int facilityId) {
        return addRecord(new Mission(1, title, briefIds, agentIds, facilityId));
    }
    @Override public Boolean updateRecord(String checkColumn, String key, String changeColumn, String value) {
        String sql = "UPDATE missions SET " + column(changeColumn) + "=? WHERE " + column(checkColumn) + " LIKE ?";
        if ("title".equals(changeColumn) && (value == null || value.isBlank())) throw new IllegalArgumentException("Title required.");
        if ("briefIds".equals(changeColumn) || "agentIds".equals(changeColumn)) value = formatIds(parseIds(value));
        if ("facilityId".equals(changeColumn) || "id".equals(changeColumn)) {
            if (Integer.parseInt(value) <= 0) throw new IllegalArgumentException("ID must be positive.");
        }
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, value); p.setString(2, key);
            return p.executeUpdate() > 0;
        } catch (SQLException e) { throw new IllegalArgumentException("Unable to update mission.", e); }
    }
    /** TSV header followed by title, briefIds CSV, agentIds CSV, facilityId. */
    @Override public int importTSV(String path, String table, String dirOut, String dbOutName) throws IOException {
        if (!tableName.equals(table)) throw new IllegalArgumentException("Expected missions table.");
        List<String> lines = Files.readAllLines(Path.of(path));
        // Validate the complete file before inserting, avoiding partial imports on invalid input.
        List<Mission> records = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            String[] fields = lines.get(i).split("\t", -1);
            if (fields.length != 4 || fields[0].isBlank()) throw new IllegalArgumentException("Expected four mission fields.");
            records.add(new Mission(1, fields[0], parseIds(fields[1]), parseIds(fields[2]), Integer.parseInt(fields[3])));
        }
        createTable();
        for (Mission mission : records) addRecord(mission);
        return records.size();
    }
    @Override public void createTable() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS missions (id INTEGER PRIMARY KEY, title TEXT NOT NULL, "
                    + "briefIds TEXT NOT NULL DEFAULT '', agentIds TEXT NOT NULL DEFAULT '', "
                    + "facilityId INTEGER NOT NULL REFERENCES facilities(id))");
        } catch (SQLException e) { throw new IllegalStateException("Unable to create missions table.", e); }
    }
}
