package org.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.model.Mission;
import org.model.Facility;
import org.model.Agent;
import org.model.Brief;

import org.controller.DebugHelper;

// TODO: Implement the JSON feauture in sql3 instead?

public class MissionManager extends IDatabaseManager<Mission>
{
    /**
     * I am using joing tables for this, the missions table cannot alone manage multiple
     * briefs and agents. All methods will be designed to reflect this. Ergo this class will look
     * rather different in its implementations than the other ones - Zaid
     *
     * Table Schema
     * CREATE TABLE IF NOT EXISTS missions 
     * (
     * id INTEGER PRIMARY KEY, 
     * title TEXT NOT NULL, 
     * facilityId INTEGER NOT NULL,
     *
     * CONSTRAINT fk_facility FOREIGN KEY (facilityId) REFERENCES facilities(id) ON DELETE CASCADE,
     *
     * )
     */

    /**
     * Junction Table Schemas:
     * Briefs:
     * CREATE TABLE IF NOT EXISTS j_mission_briefs
     * (
     * missionId INTEGER,
     * briefId INTEGER,
     * PRIMARY KEY (missionId, briefId),
     * FOREIGN KEY (missionId) REFERENCES missions(id) ON DELETE CASCADE,
     * FOREIGN KEY (briefId) REFERENCES briefs(id) ON DELETE CASCADE
     * )
     *
     * Agents:
     * CREATE TABLE IF NOT EXISTS j_mission_agents
     * (
     * missionId INTEGER,
     * agentId INTEGER,
     * PRIMARY KEY (missionId, agentId),
     * FOREIGN KEY (missionId) REFERENCES missions(id) ON DELETE CASCADE,
     * FOREIGN KEY (agentId) REFERENCES agents(id) ON DELETE CASCADE
     * )
     */
    private final String tableName = "missions";
    private final String juncBriefTableName = "j_mission_briefs";
    private final String juncAgentTableName = "j_mission_agents";
    private final String[] columns = { "id", "title", "facilityId" };

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
        return this.columns;
    }

    // TODO: Finish this function, redesign to work with junction tables
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
            // If rs is empty, we found no records and we stop and return an empty list
            if (!rs.isBeforeFirst())
            {
                return new ArrayList<>();
            }

            // This is the data stored at this table, now get data from junction tables
            while (rs.next())
            {
                int id = rs.getInt(this.columns[0]);
                String title = rs.getString(this.columns[1]);
                int facilityId = Integer.parseInt(rs.getString(this.columns[2]));
                int[] briefIds = getJuncBriefIds(id);
                int[] agentIds = getJuncAgentIds(id);
                missions.add(new Mission(id, title, briefIds, agentIds, facilityId));
            }

            // Now create a list of all the missions
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }

        // Now we can FINALLY return it
        return missions;
    }

    private int[] getJuncBriefIds(int missionId)
    {
        String sql = "SELECT * FROM " + this.juncBriefTableName
                     + " WHERE briefId = '%d'".formatted(missionId);
        List<Integer> ids = new ArrayList<>();
        try (Connection c = connect(); ResultSet rs = c.createStatement().executeQuery(sql))
        {
            while (rs.next()) {
                ids.add(rs.getInt("briefId"));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage() + "\n" + e.getStackTrace());
        }

        // Create a return array here
        int[] returnIds = new int[ids.size()];
        for (int i = 0; i < returnIds.length; i++)
        {
            returnIds[i] = ids.get(i);
        }

        return returnIds;
    }

    private int[] getJuncAgentIds(int missionId)
    {

        String sql = "SELECT * FROM " + this.juncAgentTableName
                     + " WHERE agentId = '%d'".formatted(missionId);
        List<Integer> ids = new ArrayList<>();
        try (Connection c = connect(); ResultSet rs = c.createStatement().executeQuery(sql))
        {
            while (rs.next()) {
                ids.add(rs.getInt("agentId"));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage() + "\n" + e.getStackTrace());
        }

        // Create a return array here
        int[] returnIds = new int[ids.size()];
        for (int i = 0; i < returnIds.length; i++)
        {
            returnIds[i] = ids.get(i);
        }

        return returnIds;
    }

    // TODO: Add records to junction tables to match IDs
/**
 * addRecord() + Overloads
 */

    // TODO: Test
    /**
     * Adds a record to the databse, taking in a Mission object as its value.
     */
    @Override
    public Mission addRecord(Mission value)
    {
        if (!this.tableExists(this.tableName)) createTable();
        String title = value.getTitle();
        int[] briefIdsInts = value.getBriefIds();
        int[] agentIdsInts = value.getAgentIds();
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
        DebugHelper.debugPrintln(briefIds);
    
        String agentIds = "";
        for (int i = 0; i < agentIdsInts.length; i++)
        {
            agentIds += agentIdsInts[i];
            if (i < agentIdsInts.length - 1) agentIds += ",";
        }
        DebugHelper.debugPrintln(agentIds);

        // Note the override on insert()
        int id = this.insert("INSERT INTO " + this.tableName + 
                             " (%s, %s) "
                             .formatted(this.columns[1],
                                        this.columns[2])
                             + "VALUES (?, ?)",
                             title, facilityId, briefIds, agentIds // Note: It is imperative that this order is maintained for formatting purposes
                            );

        // Now that we have all the data, we are good to go!
        return new Mission(id, title, briefIdsInts, agentIdsInts, facilityId);
    }

    // Special override to make sure junction tables are kept up to date
    @Override 
    protected int insert(String query, Object... values)
    {
        // TODO: finish
        String insertBriefSql = "INSERT INTO %s (%s, %s) VALUES (?, ?)".formatted(this.juncBriefTableName, "missionId", "briefId");
        String insertAgentSql = "INSERT INTO %s (%s, %s) VALUES (?, ?)".formatted(this.juncAgentTableName, "missionId", "agentId");

        // Add to junction tables first
        try (Connection c = connect(); PreparedStatement pstmt = c.prepareStatement(query) )
        {
            // Take this slowly
            c.setAutoCommit(false);
            int genMissionId = -1;
            // Get briefIds
            String[] briefIdStrings = ((String) values[2]).trim().split(",");
            int[] briefIds = new int[briefIdStrings.length];
            for (int i = 0; i < briefIds.length; i++) briefIds[i] = Integer.parseInt(briefIdStrings[i]);

            // Get agentIds
            String[] agentIdStrings = ((String) values[3]).trim().split(",");
            int[] agentIds = new int[agentIdStrings.length];
            for (int i = 0; i < agentIds.length; i++) agentIds[i] = Integer.parseInt(agentIdStrings[i]);

            // Get the statement working
            pstmt.setString(1, (String) values[0]); // Title
            pstmt.setInt(2, (Integer) values[1]); // FacilityId
            pstmt.executeUpdate();

            // Get new missionId as a key
            try (ResultSet rs = pstmt.getGeneratedKeys())
            {
                if (rs.next()) 
                {
                    genMissionId = rs.getInt(1);
                    DebugHelper.debugPrintln("Got mission id: " + genMissionId);
                }
            }

            // Now we can work on adding briefs and agents
            // Update brief junction
            if (genMissionId != -1 && briefIds != null)
            {
                try (PreparedStatement jstmt = c.prepareStatement(insertBriefSql))
                {

                    for (int briefId : briefIds)
                    {
                        jstmt.setInt(1, genMissionId);
                        jstmt.setInt(2, briefId);
                        jstmt.addBatch();
                        DebugHelper.debugPrintln("Added breifId: %d to missionId: %d ".formatted(briefId, genMissionId));
                    }
                    jstmt.executeBatch();
                    DebugHelper.debugPrintln("Executed brief batch successfully!");
                }
            }
            // Update agent junction
            if (genMissionId != -1 && agentIds != null)
            {
                try (PreparedStatement jstmt = c.prepareStatement(insertAgentSql))
                {
                    for (int agentId : agentIds)
                    {
                        jstmt.setInt(1, genMissionId);
                        jstmt.setInt(2, agentId);
                        jstmt.addBatch();
                        DebugHelper.debugPrintln("Added agentId: %d to missionId: %d ".formatted(agentId, genMissionId));
                    }
                    jstmt.executeBatch();
                    DebugHelper.debugPrintln("Executed agent batch successfully!");
                }
            }
            // We good
            c.commit();
            DebugHelper.debugPrintln("Comitted changes.");
            return genMissionId;
        }
        catch (SQLException e)
        {
            DebugHelper.debugPrintln(e.getMessage());
            return 0;
        }

        // Then we can just add the rest to the main table as normal
        // TODO: Update getRecords() to pull information from all tables when constructing a return Mission object
        // return super.insert(query, values[1], values[2]);
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

    // TODO: Test
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

/**
 * Private helper functions defined below
 */ 
    // TODO: Test
    /**
     * Note the Table Schemas:
     *
     * Table Schema
     * CREATE TABLE missions 
     * (
     * id INTEGER PRIMARY KEY, 
     * Title TEXT NOT NULL, 
     * facilityId INTEGER NOT NULL,
     *
     * CONSTRAINT fk_facility FOREIGN KEY (facilityId) REFERENCES facilities(id) ON DELETE CASCADE,
     * )
     */

    /**
     * Junction Table Schemas:
     * Briefs:
     * CREATE TABLE IF NOT EXISTS j_mission_briefs
     * (
     * missionId INTEGER,
     * briefId INTEGER,
     * PRIMARY KEY (missionId, briefId),
     * FOREIGN KEY (missionId) REFERENCES missions(id) ON DELETE CASCADE,
     * FOREIGN KEY (briefId) REFERENCES briefs(id) ON DELETE CASCADE
     * )
     *
     * Agents:
     * CREATE TABLE IF NOT EXISTS j_mission_agents
     * (
     * missionId INTEGER,
     * agentId INTEGER,
     * PRIMARY KEY (missionId, agentId),
     * FOREIGN KEY (missionId) REFERENCES missions(id) ON DELETE CASCADE,
     * FOREIGN KEY (agentId) REFERENCES agents(id) ON DELETE CASCADE
     * )
     */
    @Override 
    public void createTable()
    {
        String missionSchema = """
                        CREATE TABLE IF NOT EXISTS %s
                        (
                         %s INTEGER PRIMARY KEY,
                         %s TEXT NOT NULL,
                         %s INTEGER NOT NULL,
                         
                         CONSTRAINT fk_facility FOREIGN KEY (%s) REFERENCES facilities(id) ON DELETE CASCADE
                        )
                        """.formatted(
                            this.tableName,
                            this.columns[0], // id
                            this.columns[1], // title
                            this.columns[2], // facilityId
                            this.columns[2] // facilityId
                            );

        String juncAgentSchema = """
                                CREATE TABLE IF NOT EXISTS j_mission_agents
                                (
                                missionId INTEGER,
                                agentId INTEGER,
                                PRIMARY KEY (missionId, agentId),
                                FOREIGN KEY (missionId) REFERENCES missions(id) ON DELETE CASCADE,
                                FOREIGN KEY (agentId) REFERENCES agents(id) ON DELETE CASCADE
                                )
                                """;

        String juncBriefSchema = """
                                 CREATE TABLE IF NOT EXISTS j_mission_briefs
                                 (
                                 missionId INTEGER,
                                 briefId INTEGER,
                                 PRIMARY KEY (missionId, briefId),
                                 FOREIGN KEY (missionId) REFERENCES missions(id) ON DELETE CASCADE,
                                 FOREIGN KEY (briefId) REFERENCES briefs(id) ON DELETE CASCADE
                                 )
                                 """;

        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.addBatch(missionSchema);
            s.addBatch(juncAgentSchema);
            s.addBatch(juncBriefSchema);
            s.executeBatch();
        } catch (SQLException e)
        {
            DebugHelper.debugPrintln(e.getMessage());
        }
        return;
    }

    /**
     * Made junction tables to keep track of the various briefIds and agentIds that can be assigned
     * to a single mission. Invoke this function whenever you need to insert a record or else
     * database itnegrity wont be kept. - Zaid
     */
}
