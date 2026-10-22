package org.example;

import org.example.model.Agent;
import org.example.model.Brief;
import org.example.model.Facility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteAgencyDataService implements AgencyDataService {

    private final String url;

    public SqliteAgencyDataService(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Database URL cannot be blank.");
        }
        this.url = url;
        createTables();
    }

    public String getUrl() {
        return url;
    }

    @Override
    public List<Brief> getBriefs() {
        List<Brief> briefs = new ArrayList<>();
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM briefs ORDER BY id")) {
            while (rs.next()) {
                briefs.add(new Brief(rs.getInt("id"), rs.getString("title"), rs.getString("date"),
                        rs.getString("text"), (Integer) rs.getObject("mission_id")));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }
        return briefs;
    }

    @Override
    public List<Agent> getAgents() {
        List<Agent> agents = new ArrayList<>();
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM agents ORDER BY id")) {
            while (rs.next()) {
                agents.add(new Agent(rs.getInt("id"), rs.getString("name"), rs.getString("date_of_birth"),
                        rs.getString("date_of_death"), rs.getString("notes"), rs.getInt("facility_id")));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }
        return agents;
    }

    @Override
    public List<Facility> getFacilities() {
        List<Facility> facilities = new ArrayList<>();
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM facilities ORDER BY id")) {
            while (rs.next()) {
                facilities.add(new Facility(rs.getInt("id"), rs.getString("name"), rs.getString("abbreviation")));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }
        return facilities;
    }

    @Override
    public Brief addBrief(String title, String date, String text) {
        // Build the model first so invalid data is rejected before it is saved.
        new Brief(1, title, date, text, null);
        int id = insert("INSERT INTO briefs (title, date, text) VALUES (?, ?, ?)", title, date, text);
        return new Brief(id, title, date, text, null);
    }

    @Override
    public Agent addAgent(String name, String dateOfBirth, String dateOfDeath, String notes, int facilityId) {
        new Agent(1, name, dateOfBirth, dateOfDeath, notes, facilityId);
        int id = insert("INSERT INTO agents (name, date_of_birth, date_of_death, notes, facility_id) "
                + "VALUES (?, ?, ?, ?, ?)", name, dateOfBirth, dateOfDeath, notes, facilityId);
        return new Agent(id, name, dateOfBirth, dateOfDeath, notes, facilityId);
    }

    @Override
    public Facility addFacility(String name, String abbreviation) {
        new Facility(1, name, abbreviation);
        int id = insert("INSERT INTO facilities (name, abbreviation) VALUES (?, ?)", name, abbreviation);
        return new Facility(id, name, abbreviation);
    }

    private Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        c.createStatement().execute("PRAGMA foreign_keys = ON");
        return c;
    }

    private void createTables() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS facilities (id INTEGER PRIMARY KEY, "
                    + "name TEXT NOT NULL, abbreviation TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS agents (id INTEGER PRIMARY KEY, name TEXT NOT NULL, "
                    + "date_of_birth TEXT NOT NULL, date_of_death TEXT, notes TEXT, "
                    + "facility_id INTEGER NOT NULL REFERENCES facilities(id))");
            s.execute("CREATE TABLE IF NOT EXISTS briefs (id INTEGER PRIMARY KEY, title TEXT NOT NULL, "
                    + "date TEXT NOT NULL, text TEXT NOT NULL, mission_id INTEGER UNIQUE)");
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }
    }

    private int insert(String sql, Object... values) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            ps.executeUpdate();
            try (ResultSet rs = c.createStatement().executeQuery("SELECT last_insert_rowid()")) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("The database rejected the record: " + e.getMessage());
        }
    }
}
