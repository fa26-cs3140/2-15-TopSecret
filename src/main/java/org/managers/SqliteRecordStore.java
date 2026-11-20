package org.managers;

import org.model.Agent;
import org.model.Brief;
import org.model.Country;
import org.model.Facility;
import org.model.Mission;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/** Saves a complete content record and its audit entry in one SQLite transaction. */
public final class SqliteRecordStore implements RecordEditor.Store<Object> {

    private final String url;

    public SqliteRecordStore(String url) {
        this.url = url;
    }

    @Override
    public int create(Object record, RecordEditor.AuditEvent event) {
        return save(0, record, event);
    }

    @Override
    public List<Integer> createAll(List<Object> records, RecordEditor.AuditEvent event) {
        if (records.isEmpty()) {
            return List.of();
        }
        try (Connection connection = DriverManager.getConnection(url)) {
            try (var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            connection.setAutoCommit(false);
            try {
                List<Integer> ids = new ArrayList<>();
                for (Object record : records) {
                    int id = write(connection, 0, record);
                    if (record instanceof Mission mission) {
                        assignments(connection, id, mission);
                    }
                    audit(connection, id, event);
                    ids.add(id);
                }
                audit(connection, ids.size(), new RecordEditor.AuditEvent(event.username(), "IMPORT",
                        event.type(), event.timestamp()));
                connection.commit();
                return List.copyOf(ids);
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        } catch (SQLException failure) {
            throw new IllegalArgumentException("Unable to import records and audit entries.", failure);
        }
    }

    @Override
    public boolean update(int id, Object record, RecordEditor.AuditEvent event) {
        if (id <= 0) {
            throw new IllegalArgumentException("Record ID must be positive.");
        }
        return save(id, record, event) > 0;
    }

    private int save(int id, Object record, RecordEditor.AuditEvent event) {
        try (Connection connection = DriverManager.getConnection(url)) {
            try (var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            connection.setAutoCommit(false);
            try {
                int savedId = write(connection, id, record);
                if (savedId == 0) {
                    connection.rollback();
                    return 0;
                }
                if (record instanceof Mission mission) {
                    assignments(connection, savedId, mission);
                }
                audit(connection, savedId, event);
                connection.commit();
                return savedId;
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        } catch (SQLException failure) {
            throw new IllegalArgumentException("Unable to save the record and audit entry.", failure);
        }
    }

    private int write(Connection connection, int id, Object record) throws SQLException {
        String table;
        String[] columns;
        Object[] values;
        switch (record) {
            case Brief brief -> {
                table = "briefs";
                columns = new String[]{"title", "date", "text"};
                values = new Object[]{brief.getTitle(), brief.getDate(), brief.getText()};
            }
            case Agent agent -> {
                table = "agents";
                columns = new String[]{"name", "dateOfBirth", "dateOfDeath", "notes", "facilityId"};
                values = new Object[]{agent.getName(), agent.getDateOfBirth(), agent.getDateOfDeath(),
                        agent.getNotes(), agent.getFacilityId()};
            }
            case Facility facility -> {
                table = "facilities";
                columns = new String[]{"name", "abbreviation", "country_id"};
                values = new Object[]{facility.getName(), facility.getAbbreviation(),
                        facility.getCountryID() == 0 ? null : facility.getCountryID()};
            }
            case Country country -> {
                table = "countries";
                columns = new String[]{"name"};
                values = new Object[]{country.getName()};
            }
            case Mission mission -> {
                table = "missions";
                columns = new String[]{"title", "facilityId"};
                values = new Object[]{mission.getTitle(), mission.getFacilityId()};
            }
            default -> throw new IllegalArgumentException("Unsupported content record.");
        }

        String sql;
        if (id == 0) {
            String placeholders = Arrays.stream(columns).map(column -> "?").collect(Collectors.joining(", "));
            sql = "INSERT INTO " + table + " (" + String.join(", ", columns) + ") VALUES (" + placeholders + ")";
        } else {
            String setters = Arrays.stream(columns).map(column -> column + " = ?").collect(Collectors.joining(", "));
            sql = "UPDATE " + table + " SET " + setters + " WHERE id = ?";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            if (id != 0) {
                statement.setInt(values.length + 1, id);
            }
            if (statement.executeUpdate() == 0) {
                return 0;
            }
        }
        if (id != 0) {
            return id;
        }
        try (var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
            return result.getInt(1);
        }
    }

    private void assignments(Connection connection, int id, Mission mission) throws SQLException {
        replace(connection, "j_mission_briefs", "briefId", id, mission.getBriefIds());
        replace(connection, "j_mission_agents", "agentId", id, mission.getAgentIds());
    }

    private void replace(Connection connection, String table, String column, int missionId, int[] ids)
            throws SQLException {
        try (PreparedStatement remove = connection.prepareStatement("DELETE FROM " + table + " WHERE missionId = ?")) {
            remove.setInt(1, missionId);
            remove.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO " + table + " (missionId, " + column + ") VALUES (?, ?)")) {
            for (int id : ids) {
                insert.setInt(1, missionId);
                insert.setInt(2, id);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void audit(Connection connection, int id, RecordEditor.AuditEvent event) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO audit_log (username, action, content_type, content_identifier, accessed_at) "
                        + "VALUES (?, ?, ?, ?, ?)")) {
            statement.setString(1, event.username());
            statement.setString(2, event.action());
            statement.setString(3, event.type().name());
            statement.setString(4, String.valueOf(id));
            statement.setString(5, event.timestamp().toString());
            statement.executeUpdate();
        }
    }
}
