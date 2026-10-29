package org.managers;

import org.model.Brief;
import org.example.DebugHelper;

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

public class BriefManager extends IDatabaseManager<Brief>
{
    private final String url;
    /**
     * Table Schema
     * CREATE TABLE briefs (ID INTEGER PRIMARY KEY, Title TEXT, Date DATE, Text TEXT)
     */
    private final String tableName = "briefs";
    private final String[] columnNames = { "id", "title", "date", "text", "mission_id" };

    public BriefManager(String url) 
    {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Database URL cannot be blank.");
        }
        this.url = url;
        this.createTable();
    }

    @Override 
    public String getURL() 
    {
        return this.url;
    }

    @Override 
    public String getTableName()
    {
        return this.tableName;
    }
    
    @Override 
    public String[] getColumnNames()
    {
        return this.columnNames;
    }

    // TODO: Make sure this one in particular works
    /**
     * Returns an array of Brief objects that match the key in the column.
     */
    @Override 
    public Brief[] getRecords(String column, String key) 
    {
        List<Brief> briefs = new ArrayList<>();
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM " + this.tableName + 
                                                            " WHERE " + column + " = " + key + 
                                                            " ORDER BY id")) {
            while (rs.next()) {
                briefs.add(new Brief(rs.getInt("id"), rs.getString("title"), rs.getString("date"),
                        rs.getString("text"), (Integer) rs.getObject("mission_id")));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }

        Brief[] briefOut = briefs.toArray(new Brief[0]);
        return briefOut;
    }


/**
 * addRecord() + Overloads
 */

    /**
     * Adds a record to the databse, taking in a Brief object as its value.
     */
    @Override
    public Brief addRecord(Brief value)
    {
        String title = value.getTitle();
        String date = value.getDate();
        String text = value.getText();
        int id = this.insert("INSERT INTO " + this.tableName +
                             " (title, date, text) VALUES (?, ?, ?)", 
                             title, date, text
                             );
        return new Brief(id, title, date, text, null);
    }

    /**
     * Adds a record to the database, constructs a brief from input title, date, and text
     */
    public Brief addRecord(String title, String date, String text) 
    {
        // Build the model first so invalid data is rejected before it is saved.
        // We can pass a dummy ID into the temporary facility object
        Brief brief = new Brief(1, title, date, text, null);
        return this.addRecord(brief);
    }

    /**
     * Updates a record in the database. Finds the record by checking the key in checkColumn
     * and updates the value stored in changeColumn with the input value
     */
    @Override 
    public Boolean updateRecord(String checkColumn, String key, String changeColumn, String value)
    {
        String query = "UPDATE " + this.tableName + 
                      " SET " + changeColumn + " = " + value +
                      " WHERE " + checkColumn + " = " + key; 
        int rowsAffected = this.update(query, checkColumn, key, changeColumn, value);
        // Return true if the operation succeeded
        // id will return -1 if the operation failed
        return rowsAffected > 0;
    }

    /**
     * Imports a file containing tab separated values into a database.
     * Does not check to see if file is formatted correctly
     * NOTE: Assumes the existence of a header to skip
     *
     * TODO: Can this be moved into the abstract class?
     */
    @Override
    public Boolean importTSV(String pathToTSV, String tableName, String dirOut, String dbOutName) throws IOException
    {
        // Remove trailing slashes from pathToTSV in an effort to reduce error
        if (pathToTSV.charAt(pathToTSV.length() - 1) == '\\' ||
                pathToTSV.charAt(pathToTSV.length() - 1) == '/')
        {
            String tmp = pathToTSV.substring(0, pathToTSV.length() - 2);
            pathToTSV = tmp;
        }

        // Make sure JDBC exists
        try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // Check if directory and/or tsv exists
        if (!Files.exists(Path.of(dirOut))) return false;
        if (!Files.exists(Path.of(pathToTSV))) return false;

        // Helper variable for clairty on where outputs are found
        String fullOutDBPath = this.getURL().split(":")[2];
        if (!Files.exists(Path.of(fullOutDBPath))) Files.createFile(Path.of(fullOutDBPath));

        this.createTable();

        try (Connection c = connect();)
        {
            // Once the table has been created, we change the query
            // to insert into the new table
            String query = "INSERT INTO " + tableName + 
                   " (%s, %s, %s) VALUES (?, ?, ?)".formatted(this.columnNames[1],
                                                              this.columnNames[2],
                                                              this.columnNames[3]);

            PreparedStatement statement = c.prepareStatement(query);
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
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

/**
 * Private helper functions defined below
 */
    @Override 
    protected void createTable() 
    {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY, %s TEXT NOT NULL, "
                    + "%s TEXT NOT NULL, %s TEXT NOT NULL, mission_id INTEGER UNIQUE)"
                    .formatted(this.tableName, this.columnNames[0], this.columnNames[1], this.columnNames[2], this.columnNames[3]));
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }
    }
}
