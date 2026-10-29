package org.managers;

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

public class FacilityManager extends IDatabaseManager<Facility>
{
    /**
     * Table Schema
     * CREATE TABLE facilities (id INTEGER PRIMARY KEY, 
     * name TEXT NOT NULL, abbreviation TEXT NOT NULL, mission_id INTEGER UNIQUE)
     */
    private final String tableName = "facilities";
    private final String[] columnNames = { "id", "name", "abbreviation", "mission_id" };

    public FacilityManager(String url)
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

    /**
     * Returns an array of Facility objects that match the key in the column.
     */
    @Override 
    public Facility[] getRecords(String column, String key)
    {
        List<Facility> facilities = new ArrayList<>();
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM " + this.tableName + 
                                                            " WHERE " + column + " = " + key + 
                                                            " ORDER BY " + this.columnNames[0])) {
            while (rs.next()) {
                facilities.add(new Facility(rs.getInt(this.columnNames[0]), 
                            rs.getString(this.columnNames[1]),
                            rs.getString(this.columnNames[2])
                            ));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }

        Facility[] facilityOut = facilities.toArray(new Facility[0]);
        return facilityOut;
    }

    /**
     * Adds a record to the databse, taking in a Facility object as its value.
     *
     * Returns a copy of the facility object with the correct ID
     */
    @Override
    public Facility addRecord(Facility value)
    {
        String name = value.getName();
        String abbreviation = value.getAbbreviation();
        int id = this.insert("INSERT INTO " + this.tableName + 
                             " (%s, %s)".formatted(this.columnNames[1], this.columnNames[2]),
                             name, abbreviation
                );
        return new Facility(id, name, abbreviation);
    }

    /**
     * Adds a record to the database, constructs a facility from input title, date, and text
     *
     * Returns a copy of the generated faclity object with the correct ID
     */
    public Facility addRecord(String name, String abbreviation)
    {
        // Build the model first so invalid data is rejected before it is saved.
        // We can pass a dummy ID into the temporary facility object
        Facility facility = new Facility(1, name, abbreviation);
        return this.addRecord(facility);
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

    @Override 
    public Boolean importTSV(String pathToTSV, String tableName, String dirOut, String dbOutName) throws IOException
    {
        // TODO: Write this function
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

        // columnName values:
        // 0 - id, 1 - name, 2 - abbreviation, 3 - mission_id
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
     * Private helper functions below
     */
    

    @Override 
    protected void createTable() 
    {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY, %s TEXT NOT NULL, "
                    + "%s TEXT NOT NULL, mission_id INTEGER UNIQUE)"
                    .formatted(this.tableName, this.columnNames[0], this.columnNames[1], this.columnNames[2]));
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }
    }
}
