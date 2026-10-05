package org.example;

import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class DatabaseManager
{
    String sqlURL = "jdbc:sqlite:data/mission_briefs.db";
    String username = "user";
    String password = "pwd";

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

    public int setRecord(String key, String data)
    {
        return 0;
    }

    public int setColumn(String header, String data)
    {
        return 0;
    }

    public int insertRecord(String key, String data)
    {
        return 0;
    }

    public int insertColumn(String header, String data)
    {
        return 0;
    }

    public String[] getRecords(String url, String header)
    {
        return null;
    }

    public String getColumns(String header)
    {
        return null;
    }

    // Creates a database from an input TSV file
    // I decided not to use any other dependent methods for this to make
    // sure that any error could be isolated within this method
    // NOTE: Do not include trailing '/' at the end of the dirOut path
    public Boolean createMissionDatabaseFromTSV(String pathToTSV, String dirOut, String dbOutName) throws IOException
    {
        // Making sure JDBC exists
        try 
        {
            DebugHelper.debugPrintln("Checking JDBC exists.");
            Class.forName("org.sqlite.JDBC");
            DebugHelper.debugPrintln("JDBC exists.");
        }
        catch (ClassNotFoundException e)
        {
            DebugHelper.debugPrintln("JDBC Class not found. Check dependencies.");
            return false;
        }

        // System.out.println("[" + this.getFunctionName() + "]: Creating db...");
        DebugHelper.debugPrintln("Creating db...");

        // Check if directory and/or tsv exists
        if (!Files.exists(Path.of(dirOut)))
        {
            // System.out.println("[" + this.getFunctionName() + "]: output directory not found.");
            DebugHelper.debugPrintln("Output directory not found.");
            return false;
        }

        if (!Files.exists(Path.of(pathToTSV)))
        {
            // System.out.println("[" + this.getFunctionName() + "]: output directory not found.");
            DebugHelper.debugPrintln("Input .tsv not found.");
            return false;
        }
        
        // Helper variable for clairty on where outputs are found
        String fullOutDBPath = dirOut + "/" + dbOutName;
        
        // Create a URL to output the file
        String jdbcURL = "jdbc:sqlite:" + fullOutDBPath;

        // Create table to be inserted, following mission table format (Name, Date, Text)
        String query; 
        if (!Files.exists(Path.of(fullOutDBPath))) 
        {
            // Create the file
            Files.createFile(Path.of(fullOutDBPath));
            DebugHelper.debugPrintln("File made: " + fullOutDBPath);
        }
        // 
        
        // Once the table has been created, we change the query
        // to insert into the new table
        DebugHelper.debugPrintln("Creating table...");
        query = "CREATE TABLE IF NOT EXISTS mission_briefs (Title TEXT, Date DATE, Text TEXT)";
        this.createQuery(jdbcURL, query);

        DebugHelper.debugPrintln("Trying connection...");
        try (
                Connection conn = DriverManager.getConnection(jdbcURL, this.username, this.password);
            )
        {
            query = "INSERT INTO mission_briefs (Title, Date, Text) VALUES (?, ?, ?)";
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
            DebugHelper.debugPrintln("Datase successfuly made.");

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

    // Generates a compilable query from an input string in SQLite3 query syntax
    // Returns whether or not the query was completed as a boolean (true if so, false if not)
    private Boolean createQuery(String url, String query)
    {
        DebugHelper.debugPrintln("Trying connection...");
        try // Conditions for the try statement
            (
                Connection conn = DriverManager.getConnection(url, this.username, this.password);
                Statement statement = conn.createStatement();
            )
        {
            statement.setQueryTimeout(30);
            ResultSet rs = statement.executeQuery(query);
            return true;
        }
        catch (SQLException e)
        {
            this.printSQLDiagonistics(e);
            return false;
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
