package org.example;

import org.model.Agent;
import org.model.Brief;
import org.model.Facility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager implements AgencyDataService
{
    private final String url;

    public DatabaseManager(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Database URL cannot be blank.");
        }
        this.url = url;
        createTables();
    }

    public String getURL() {
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

    public Boolean tableExists(String table)
    {
        try (
                Connection conn = DriverManager.getConnection(this.url);
            )
        {
            DatabaseMetaData meta = conn.getMetaData();

            try (ResultSet rs = meta.getTables(null, null, table, new String [] {"TABLE"})) {
                return rs.next();
            }

        }
        catch (SQLException e)
        {
            return false;
        }
    }
    
    public Boolean importTSV(String pathToTSV, String tableName, String dirOut, String dbOutName) throws IOException
    {
        // Remove trailing slashes from pathToTSV in an effort to reduce error
        if (pathToTSV.charAt(pathToTSV.length() - 1) == '\\' ||
            pathToTSV.charAt(pathToTSV.length() - 1) == '/')
        {
            String tmp = pathToTSV.substring(0, pathToTSV.length() - 2);
            pathToTSV = tmp;
        }
        // Making sure JDBC exists, giving me hell trying to fix
        try 
        {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e)
        {
            DebugHelper.debugPrintln("JDBC Class not found. Check dependencies.");
            return false;
        }

        // Check if directory and/or tsv exists
        if (!Files.exists(Path.of(dirOut))) return false;

        if (!Files.exists(Path.of(pathToTSV))) return false;
        
        // Helper variable for clairty on where outputs are found
        String fullOutDBPath = dirOut + "/" + dbOutName;
        
        // Create a URL to output the file
        String jdbcURL = "jdbc:sqlite:" + fullOutDBPath;

        // Create table to be inserted, following mission table format (Name, Date, Text)
        String query; 
        if (!Files.exists(Path.of(fullOutDBPath))) Files.createFile(Path.of(fullOutDBPath));
        
        query = "CREATE TABLE IF NOT EXISTS " + tableName + " (Title TEXT, Date DATE, Text TEXT)";
        this.createQuery(jdbcURL, query);

        try (
                Connection conn = DriverManager.getConnection(jdbcURL);
            )
        {
            // Once the table has been created, we change the query
            // to insert into the new table
            query = "INSERT INTO " + tableName + " (Title, Date, Text) VALUES (?, ?, ?)";

            PreparedStatement statement = conn.prepareStatement(query);
            BufferedReader reader = new BufferedReader(new FileReader(pathToTSV));
            String readText;

            // Skip header
            reader.readLine();

            // Read out all the information from the TSV
            while ((readText = reader.readLine()) != null)
            {
                // Split around the tabs. Title in 0, Date in 1, Text in 2
                String[] fields = readText.split("\t");

                statement.setString(1, fields[0]);
                statement.setDate(2, Date.valueOf(fields[1]));
                statement.setString(3, fields[2]);

                // Add data to statement queries to an execution.
                statement.addBatch();
            }

            // After obtaining all the necessary statements, execute
            statement.executeBatch();
            // System.out.println("[" + this.getFunctionName() + "]: Database made.");
            // Final stuff
            reader.close();
            return true;
        }
        catch (SQLException e)
        {
            return false;
        }
    }

    // Generates a compilable query from an input string in SQLite3 query syntax
    // Returns a ResultSet object containing the retrieved query items
    private ResultSet createQuery(String url, String query)
    {
        try // Conditions for the try statement
            (
                Connection conn = DriverManager.getConnection(url);
            )
        {
            Statement statement = conn.createStatement();
            statement.setQueryTimeout(30);
            ResultSet rs = statement.executeQuery(query);
            return rs;
        }
        catch (SQLException e)
        {
            return null;
        }
    }
}
