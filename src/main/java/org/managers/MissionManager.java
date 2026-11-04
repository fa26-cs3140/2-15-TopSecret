package org.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.model.Mission;
import org.controller.DebugHelper;


public class MissionManager extends IDatabaseManager<Mission>
{
    /**
     * Table Schema
     * CREATE TABLE missions (ID INTEGER PRIMARY KEY, Title TEXT NOT NULL, briefIds INTEGER NOT NULL REFERENCES briefs (id),
     * agentIds INTEGER REFERENCES agent (id), facilityId INTEGER REFERENCES facilities (id) )
     */
    private final String tableName = "missions";
    private final String[] columnNames = { "id", "title", "briefIds", "agentIds", "facilityId" };

    public MissionManager(String url)
    {
        this.url = url;
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

    /**
     * Columns contain data formatted as follows
     * 0. id, 1. title, 2. briefIds*, 3. agentIds, 4. facilityIds
     * Starried entries can be null
     */
    @Override 
    public String[] getColumnNames()
    {
        return this.columnNames;
    }

    // TODO: Finish this function
    /**
     * Returns an array of Mission objects that match the key in the column
     */
    @Override 
    public List<Mission> getRecords(String column, String key)
    {
        List<Mission> missions = new ArrayList<>();
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM " + this.tableName + 
                                                            " WHERE " + column + " LIKE " + "\'" + key + "\'" + 
                                                            " ORDER BY id")) {

            // TODO: Redesign Mission type to contain all requisite data
            while (rs.next()) {
                missions.add(new Mission(rs.getInt("id"), rs.getInt("facilityIds")));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }

        return missions;
    }

/**
 * addRecord() + Overloads
 */

    /**
     * Adds a record to the databse, taking in a Mission object as its value.
     */
    @Override
    public Mission addRecord(Mission value)
    {
        return null;
    }
    
    // TODO: Change signature when details of Mission type are finalised
    /**
     * Adds a record to the database, constructs a brief from input title, date, and text
     */
    public Mission addRecord(String title, String date, String text) 
    {
        return null;
    }

/**
 * End addRecord()
 */

    // TODO: Implement
    /**
     * Updates a record in the database. Finds the record by checking the key in checkColumn
     * and updates the value stored in changeColumn with the input value
     */
    @Override 
    public Boolean updateRecord(String checkColumn, String key, String changeColumn, String value)
    {
        return false;
    }

    // TODO: Implement
    /**
     * Imports a file containing tab separated values into a database.
     * Does not check to see if file is formatted correctly
     * Returns the number of records added to the database
     * NOTE: Assumes the existence of a header to skip
     */
    @Override
    public int importTSV(String pathToTSV, String tableName, String dirOut, String dbOutName) throws IOException
    {
        return 0;
    }

    // TODO: Implement
/**
 * Private helper functions defined below
 */ 
    /**
     * Note the Table Schema
     * CREATE TABLE missions (ID INTEGER PRIMARY KEY, Title TEXT NOT NULL, briefIds INTEGER NOT NULL REFERENCES briefs (id),
     * agentIds INTEGER REFERENCES agent (id), facilityId INTEGER REFERENCES facilities (id) )
     */
    @Override 
    public void createTable()
    {
        return;
    }
}
