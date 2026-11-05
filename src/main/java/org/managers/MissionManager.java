package org.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.model.Agent;
import org.model.Mission;
import org.controller.DebugHelper;


public class MissionManager extends IDatabaseManager<Mission>
{
    /**
     * Table Schema
     * CREATE TABLE missions 
     * (
     * ID INTEGER PRIMARY KEY, 
     * Title TEXT NOT NULL, 
     * briefIds INTEGER,
     * agentIds INTEGER, 
     * facilityId INTEGER NOT NULL,
     *
     * CONSTRAINT fk_briefs FOREIGN KEY (briefIds) REFERENCES briefs(id) ON DELETE CASCADE,
     * CONSTRAINT fk_agents FOREIGN KEY (agentIds) REFERENCES agents(id) ON DELETE CASCADE,
     * CONSTRAINT fk_facility FOREIGN KEY (facilityId) REFERENCES facilities(id) ON DELETE CASCADE,
     *
     * )
     */
    private final String tableName = "missions";
    private final String[] columnNames = { "id", "title", "briefIds", "agentIds", "facilityId" };

    public MissionManager(String url)
    {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Database URL cannot be blank.");
        }
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
            int id = rs.getInt(this.columnNames[0]);
            String title = rs.getString(this.columnNames[1]);
            // Parse out briefIds and agentIds differently because
            // they can contain multiple values
            // Parse out briefIds
            String[] briefIdsParts = rs.getString(this.columnNames[2]).split(",");
            List<Integer> briefIdsList = new ArrayList<>();
            for (String part : briefIdsParts)
            {
                briefIdsList.add(Integer.parseInt(part.trim()));
            }

            // Parse out agentIds
            String[] agentIdsParts = rs.getString(this.columnNames[3]).split(",");
            List<Integer> agentIdsList = new ArrayList<>();
            for (String part : agentIdsParts)
            {
                agentIdsList.add(Integer.parseInt(part.trim()));
            }

            // Now make them arrays
            int[] briefIds = new int[briefIdsList.size()];
            int[] agentIds = new int[agentIdsList.size()];
            for (int i = 0; i < briefIds.length; i++) briefIds[i] = briefIdsList.get(i);
            for (int i = 0; i < agentIds.length; i++) agentIds[i] = agentIdsList.get(i);

            int facilityId = Integer.parseInt(rs.getString(this.columnNames[4]));

            // Now create a list of all the missions
            while (rs.next()) {
                missions.add(
                        new Mission(id, title, 
                                    briefIds, agentIds,
                                    facilityId));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }

        // Now we can FINALLY return it
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
        String title = value.getTitle();
        int[] briefIdsInts = value.getBriefIds();
        int[] agentIdsInts = value.getBriefIds();
        int facilityId = value.getFacilityId();

        // We need to do some extra formatting and parsing to
        // properly insert the mutliple values of briefs and agents
        // assigned to a mission
        String briefIds = "";
        for (int i = 0; i < briefIdsInts.length; i++)
        {
            briefIds += briefIdsInts[i];
            if (i < briefIdsInts.length - 1) briefIds += ",";
        }

        String agentIds = "";
        for (int i = 0; i < agentIdsInts.length; i++)
        {
            agentIds += agentIdsInts[i];
            if (i < agentIdsInts.length - 1) agentIds += ",";
        }
        // TODO: This almost certainly does not work. Make sure you are aware
        // how to actually add MULTIPLE items into an sql column
        int id = this.insert("INSERT INTO " + this.tableName + 
                             " (%s, %s, %s, %s) "
                             .formatted(this.columnNames[1], this.columnNames[2],
                                        this.columnNames[3], this.columnNames[4])
                             + "VALUES (?, ?, ?, ?)",
                             title, briefIds, agentIds, facilityId
                            );

        // Now that we have all the data, we are good to go!
        return new Mission(id, title, briefIdsInts, agentIdsInts, facilityId);
    }
    
    // TODO: Change signature when details of Mission type are finalised
    /**
     * Adds a record to the database, constructs a brief from input title, date, and text
     */
    public Mission addRecord(String title, int[] briefIds, int[] agentIds, int facilityId) 
    {
        Mission mission = new Mission(1, title, briefIds, agentIds, facilityId);
        return this.addRecord(mission);
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
        String query = "UPDATE " + this.tableName + 
                      " SET " + changeColumn + " = " + "\'" + value +"\'" +
                      " WHERE " + checkColumn + " LIKE " + "\'" + key + "\'"; 
        int rowsAffected = this.update(query, checkColumn, key, changeColumn, value);

        return rowsAffected > 0;
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
     * Note the Table Schema:
     *
     * Table Schema
     * CREATE TABLE missions 
     * (
     * id INTEGER PRIMARY KEY, 
     * Title TEXT NOT NULL, 
     * briefIds INTEGER,
     * agentIds INTEGER, 
     * facilityId INTEGER NOT NULL,
     *
     * CONSTRAINT fk_briefs FOREIGN KEY (briefIds) REFERENCES briefs(id) ON DELETE CASCADE,
     * CONSTRAINT fk_agents FOREIGN KEY (agentIds) REFERENCES agents(id) ON DELETE CASCADE,
     * CONSTRAINT fk_facility FOREIGN KEY (facilityId) REFERENCES facilities(id) ON DELETE CASCADE,
     *
     * )
     */
    @Override 
    public void createTable()
    {
        String schema = """
                        CREATE TABLE %s
                        (
                         %s INTEGER PRIMARY KEY,
                         %s TEXT NOT NULL,
                         %s INTEGER,
                         %s INTEGER,
                         %s INTEGER NOT NULL
                         
                         CONSTRAINT fk_briefs FOREIGN KEY (%s) REFERENCES briefs(id) ON DELETE CASCADE,
                         CONSTRAINT fk_agents FOREIGN KEY (%s) REFERENCES agents(id) ON DELETE CASCADE,
                         CONSTRAINT fk_facility FOREIGN KEY (%s) REFERENCES facilities(id) ON DELETE CASCADE,
                        )
                        """.formatted(
                            this.tableName,
                            this.columnNames[0], // id
                            this.columnNames[1], // title
                            this.columnNames[2], // briefIds
                            this.columnNames[3], // agentids
                            this.columnNames[4], // facilityId
                            
                            this.columnNames[2],
                            this.columnNames[3],
                            this.columnNames[4]
                            );
        return;
    }
}
