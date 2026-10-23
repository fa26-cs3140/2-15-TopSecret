package org.example;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;

public class DatabaseManager
{
    private String sqlURL = "jdbc:sqlite:data/mission_briefs.db";
    private String username = "user";
    private String password = "pwd";

    private final String userTableName = "users";
    private final String missionBriefTableName = "mission_briefs";

    public DatabaseManager()
    {
    }

    public DatabaseManager(String url)
    {
        this.setURL(url);
    }

    public void setURL(String url)
    {
        this.sqlURL = url;
    }

    public String getURL()
    {
        return this.sqlURL;
    }
    
    /**
     * Returns all matching records matching a given value in a column as a String[]
     * Points to a database defined in url.
     * Each item in the string is formatted as a tab separated table
     *
     *
     * @param url pointer to the database 
     * @param column name of the column to search 
     * @param value value to match
     */
    public String[] getRecords(String url, String table, String column, String value)
    {
        String query = "SELECT Title, Date, Text FROM " + table + " WHERE " + column + " LIKE ?";
        // Get a ResultSet containing matching records
        // ResultSet rs = this.createQuery(url, query);
        ResultSet rs = null;
        try (
                Connection conn = DriverManager.getConnection(url, this.username, this.password);
            )
        {
            PreparedStatement prepStatement = conn.prepareStatement(query);

            // Bind values
            prepStatement.setString(1, value);

            rs = prepStatement.executeQuery();

            if (rs == null) return null;
            try
            {
                // Create an array and then parse out the records
                // Much time spent here reading the documentation
                // And perhaps much wasted in an effort to understand
                // Update: HAHA! IT WORKS! IM A GOD!
                ArrayList<String> temp = new ArrayList<String>();
                while (rs.next())
                {
                    String record = String.format(
                            "%s\t%s\t%s",
                            rs.getString("Title"),
                            rs.getDate("Date").toString(),
                            rs.getString("Text")
                            );

                    temp.add(record);
                }

                String[] out = temp.toArray(new String[0]);
                return out;
            }
            catch (SQLException e)
            {
                this.printSQLDiagonistics(e);
                return null;
            }
        } 
        catch (SQLException e)
        {
            this.printSQLDiagonistics(e);
            return null;
        }
    }

    /**
     * Checks to see if a table by the name 'tableName' exists, returns true if true
     * 
     * @param tableName the name of the table
     */
    public Boolean tableExists(String table)
    {
        try (
                Connection conn = DriverManager.getConnection(this.sqlURL, this.username, this.password);
            )
        {
            DatabaseMetaData meta = conn.getMetaData();

            try (ResultSet rs = meta.getTables(null, null, table, new String [] {"TABLE"})) {
                return rs.next();
            }

        }
        catch (SQLException e)
        {
            this.printSQLDiagonistics(e);
            return false;
        }
    }

    /**
     * Returns all matching records matching a given value in a column as a String[]
     * Points to a database defined in the objects sqlURL field.
     * Each item in the string is formatted as a tab separated table
     *
     *
     * @param column name of the column to search 
     * @param value value to match
     */
    public String[] getRecords(String table, String column, String key)
    {
        return this.getRecords(this.sqlURL, table, column, key);
    }

    /**
     * Returns all values stored in a column as String[]
     * Searches the databse pointed to the url parameter
     *
     * @param url pointer to database to search
     * @param column name of column to pull
     */
    public String[] getColumn(String url, String table, String column)
    {
        // Get the records which match the search
        String query = "SELECT " + column + " FROM " + table;

        // Pipe it into an sql statement
        try (
                Connection conn = DriverManager.getConnection(url, this.username, this.password);
            )
        {
            PreparedStatement prepStatement = conn.prepareStatement(query);
            ResultSet rs = prepStatement.executeQuery();

            // No results, return a null object
            if (rs == null) return null;
            
            ArrayList<String> tempout = new ArrayList<String>();
            while (rs.next())
            {
                int columntype = rs.getMetaData().getColumnType(1);
                String rs_value;
                if (columntype == Types.DATE) rs_value = rs.getDate(column).toString();
                else rs_value = rs.getString(column);
                tempout.add(rs_value);
            }

            String[] out = tempout.toArray(new String[0]);
            return out;
        }
        catch (SQLException e)
        {
            this.printSQLDiagonistics(e);
            return null;
        }
    }

    /**
     * Returns all values stored in a column as String[]
     * Searches the databse pointed to by the instance's sqlURL field
     *
     * @param column name of column to pull
     */
    public String[] getColumn(String table, String column)
    {
        return this.getColumn(this.sqlURL, table, column);
    }

    /** 
     * Creates a database from an input TSV file
     * I decided not to use any other dependent methods for this to make
     * sure that any error could be isolated within this method
     * NOTE: Do not include trailing '/' at the end of the dirOut path
     *
     * @param pathToTSV string path to the TSV to import 
     * @param dirOut string path to the output directory 
     * @param dbOutName string name of the output databse 
     * @throws IOException
     */
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

        // System.out.println("[" + this.getFunctionName() + "]: Creating db...");

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
                Connection conn = DriverManager.getConnection(jdbcURL, this.username, this.password);
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
            this.printSQLDiagonistics(e);
            return false;
        }
    }

    /**
     * Returns table name containing mission briefs
     */
    public String getBriefsTableName()
    {
        return this.missionBriefTableName;
    }

    /**
     * Returns table name containing users
     */
    public String getUsersTableName()
    {
        return this.userTableName;
    }

    /**
     * End of public functions
     */

    // Generates a compilable query from an input string in SQLite3 query syntax
    // Returns a ResultSet object containing the retrieved query items
    private ResultSet createQuery(String url, String query)
    {
        try // Conditions for the try statement
            (
                Connection conn = DriverManager.getConnection(url, this.username, this.password);
            )
        {
            Statement statement = conn.createStatement();
            statement.setQueryTimeout(30);
            ResultSet rs = statement.executeQuery(query);
            return rs;
        }
        catch (SQLException e)
        {
            this.printSQLDiagonistics(e);
            return null;
        }
    }

    // General purpose function for printing out all SQLException diagonistics
    private void printSQLDiagonistics(SQLException e)
    {
        System.err.println("Error processing query.");
        System.err.println("State: " + e.getSQLState());
        System.err.println("Error: " + e.getErrorCode());
        System.err.println("Message: " + e.getMessage());
    }
}
