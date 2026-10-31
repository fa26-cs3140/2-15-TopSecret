package org.managers;

import org.model.Agent;
import org.example.DebugHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AgentManager extends IDatabaseManager<Agent>
{
    /**
     * Table Schema
     * CREATE TABLE agents (id INTEGER PRIMARY KEY, 
     * name TEXT NOT NULL, dateOfBirth DATE NOT NULL, dateOfDeath DATE,
     * notes TEXT, facilityId INTEGER, 
     * FOREIGN KEY (facilityId) REFERENCES facilities (id)
     */
    private final String tableName = "agents";
    private final String[] columnNames = { "id", "name", "dateOfBirth", "dateOfDeath", "notes", "facilityId" };

    public AgentManager(String url)
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
     * Returns an array of Agent objects that match the key in the column.
     */
    @Override 
    public List<Agent> getRecords(String column, String key)
    {
        List<Agent> agents = new ArrayList<>();
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM " + this.tableName + 
                                                            " WHERE " + column + " LIKE " + "\'" + key + "\'" + 
                                                            " ORDER BY " + this.columnNames[0])) {
            while (rs.next()) {
                agents.add(new Agent(
                            rs.getInt(this.columnNames[0]),
                            rs.getString(this.columnNames[1]),
                            rs.getString(this.columnNames[2]),
                            rs.getString(this.columnNames[3]),
                            rs.getString(this.columnNames[4]),
                            rs.getInt(this.columnNames[5])
                            ));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }

        return agents;
    }

    /**
     * Adds a record to the databse, taking in a Agent object as its value.
     *
     * Returns a copy of the Agent object with the correct ID
     */
    @Override
    public Agent addRecord(Agent value)
    {
        String name = value.getName();
        String dateOfBirth = value.getDateOfBirth();
        String dateOfDeath = value.getDateOfDeath();
        String notes = value.getNotes();
        int facilityId = value.getFacilityId();
        int id = this.insert("INSERT INTO " + this.tableName + 
                             " (%s, %s, %s, %s, %s) "
                             .formatted(this.columnNames[1], this.columnNames[2], this.columnNames[3],
                                        this.columnNames[4], this.columnNames[5])
                             + "VALUES (?, ?, ?, ?, ?)",
                             name, dateOfBirth, dateOfDeath, notes, facilityId
                );
        return new Agent(id, name, dateOfBirth, dateOfDeath, notes, facilityId);
    }

    /**
     * Adds a record to the database, constructs an agent from input title, date, and text
     *
     * Returns a copy of the generated faclity object with the correct ID
     */
    public Agent addRecord(String name, String dateOfBirth, String dateOfDeath, String notes, int facilityId)
    {
        // Build the model first so invalid data is rejected before it is saved.
        // We can pass a dummy ID into the temporary facility object
        Agent agent = new Agent(1, name, dateOfBirth, dateOfDeath, notes, facilityId);
        return this.addRecord(agent);
    }

    /**
     * Updates a record in the database. Finds the record by checking the key in checkColumn
     * and updates the value stored in changeColumn with the input value
     */
    @Override 
    public Boolean updateRecord(String checkColumn, String key, String changeColumn, String value)
    {
        String query = "UPDATE " + this.tableName + 
                      " SET " + changeColumn + " = " + "\'" + value +"\'" +
                      " WHERE " + checkColumn + " LIKE " + "\'" + key + "\'"; 
        int rowsAffected = this.update(query, checkColumn, key, changeColumn, value);
        // Return true if the operation succeeded
        // id will return -1 if the operation failed
        return rowsAffected > 0;
    }

    
    /**
     * Imports a file containing tab separated values into a database.
     * Does not check to see if file is formatted correctly
     * Returns the nuumber of records added to the database
     * NOTE: Assumes the existence of a header to skip
     *
     * NOTE: This function is unique in that only the first two parameters are necessary,
     * so make sure to check that the length of the data array actually contains the necessary
     * data to import if necessary (i.e. date of death and notes). FacilityID must always be
     * included
     */
    @Override
    public int importTSV(String pathToTSV, String tableName, String dirOut, String dbOutName) throws IOException
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
        if (!Files.exists(Path.of(dirOut))) return 0;
        if (!Files.exists(Path.of(pathToTSV))) return 0;

        this.createTable();
        BufferedReader reader = new BufferedReader(new FileReader(pathToTSV));
        String readText;

        // Skip header
        reader.readLine();

        int numRecords = 0;
        while ((readText = reader.readLine()) != null)
        {
            // Each column should correspond with the requisite value, apart from id
            String[] data = readText.split("\t");
            
            String name = data[0];
            String dateOfBirth = data[1];
            String dateOfDeath = data[2];
            String notes = data[3];
            int facilityId = Integer.parseInt(data[4]);

            this.addRecord(name, dateOfBirth, dateOfDeath, notes, facilityId) ;
            numRecords++;
        }

        reader.close();
        return numRecords;
    }

    /**
     * Private helper functions defined below
     */
    @Override
    public void createTable()
    {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY, %s TEXT NOT NULL, %s DATE NOT NULL, "
                    .formatted(this.tableName, this.columnNames[0], this.columnNames[1], this.columnNames[2])
                    + "%s DATE, %s TEXT, %s INTEGER UNIQUE, FOREIGN KEY (%s) REFERENCES facilities (id) )"
                    .formatted(this.columnNames[3], this.columnNames[4], this.columnNames[5], this.columnNames[5]));
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }
    }
}
