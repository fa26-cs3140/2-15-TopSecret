package org.managers;

import org.model.Facility;
import org.model.Countries;
import org.model.Country;
import org.controller.DebugHelper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
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
     * CREATE TABLE facilities 
     * (
     *  id INTEGER PRIMARY KEY, 
     *  name TEXT NOT NULL, 
     *  abbreviation TEXT NOT NULL, 
     *  country_id INTEGER,
     *  mission_id INTEGER,
     *
     *  CONSTRAINT fk_country FOREIGN KEY (country_id) REFERENCES countries(id)
     *  )
     *
     *  The createTable() function will also load all country data, which will be stored 
     *  as an enumerator in memory
     *
     *
     *  Country Table Schema
     *  CREATE TABLE countries
     *  (
     *  id INTEGER PRIMARY KEY,
     *  name TEXT NOT NULL
     *  )
     */

    // Perhaps a new table is in order.
    // Could make a table containing nothing but countries and their IDS,
    // at which point searching for matching names would be nice.
    // Basically a search query would query the country table, get the id
    // then query the faclity table for all facilities with a matching 
    // country id. 
    //
    //
    private final String tableName = "facilities";
    private final String[] columnNames = { "id", "name", "abbreviation", "country_id", "mission_id" };

    public FacilityManager(String url)
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
     * 0. id, 1. name, 2. abbreviation, 3. country_id, 4. mission_id*
     * Starred entries can be null
     */
    @Override 
    public String[] getColumnNames()
    {
        return this.columnNames;
    }

    /**
     * Returns an array of Facility objects that match the key in the column.
     */
    @Override 
    public List<Facility> getRecords(String column, String key)
    {
        List<Facility> facilities = new ArrayList<>();
        try (Connection c = connect();
             ResultSet rs = c.createStatement().executeQuery("SELECT * FROM " + this.tableName + 
                                                            " WHERE " + column + " LIKE " + "\'" + key + "\'" + 
                                                            " ORDER BY " + this.columnNames[0])) {
            while (rs.next()) {
                facilities.add(new Facility(rs.getInt(this.columnNames[0]), 
                            rs.getString(this.columnNames[1]),
                            rs.getString(this.columnNames[2]),
                            rs.getInt(this.columnNames[3])
                            ));
            }
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }

        return facilities;
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
        int country = value.getCountryID();
        int id = this.insert("INSERT INTO " + this.tableName + 
                             " (%s, %s, %s) ".formatted(this.columnNames[1], this.columnNames[2], this.columnNames[3])
                             + "VALUES (?, ?, ?)",
                             name, abbreviation, country
                );
        return new Facility(id, name, abbreviation, country);
    }

    /**
     * Adds a record to the database, constructs a facility from input title, date, and text
     *
     * Returns a copy of the generated faclity object with the correct ID
     */
    public Facility addRecord(String name, String abbreviation, int country)
    {
        // Build the model first so invalid data is rejected before it is saved.
        // We can pass a dummy ID into the temporary facility object
        Facility facility = new Facility(1, name, abbreviation, country);
        return this.addRecord(facility);
    }

    /**
     * Updates a record in the database. Finds the record by checking the key in checkColumn
     * and updates the value stored in changeColumn with the input value
     */
    @Override 
    public Boolean updateRecord(String checkColumn, String key, String changeColumn, String value)
    {
        return this.updateColumn(checkColumn, key, changeColumn, value);
    }

    /**
     * Returns a Country by matching a name, get country names from enum
     */
    public Country getCountry(Countries countryEnum)
    {
        return this.getCountry(countryEnum.getName());
        // List<Facility> facs = new ArrayList<>();
        // String name = countryEnum.getName();
        // String query = "SELECT name FROM countries WHERE name LIKE '?'";
        // int outId = -1;
        // String outName = "";

        // // Get country id from facility
        // try (Connection c = connect(); PreparedStatement s = c.prepareStatement(query))
        // {
        //     s.setString(1, name);
        //     ResultSet rs = s.executeQuery();
        //     // Pass header
        //     rs.next();
        //     outName = rs.getString("name");
        //     outId = rs.getInt("id");
        //     
        // } catch (SQLException e) {
        //     return null;
        // }

        // // Get facilities
        // facs = this.getRecords(columnNames[3], "" + outId);
        // Facility[] outFacs = facs.toArray(new Facility[0]);
        // Country out = new Country(outName, outId, outFacs);

        // return out;
    }

    public Country getCountry(int countryId)
    {
        if (countryId <= 0 || countryId > 196) return null;

        List<Facility> facs = new ArrayList<>();
        String query = "SELECT id, name FROM countries WHERE id = ?";
        String outName = "";
        int outId = -1;
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(query))
        {
            s.setInt(1, countryId);
            try (ResultSet rs = s.executeQuery())
            {
                if (rs.next()) // If valid query
                {
                    outName = rs.getString("name");
                    outId = rs.getInt("id");
                } 
                else 
                {
                    return null;
                }
            }

        } catch (SQLException e) {
            return null;
        }

        facs = this.getRecords(columnNames[3], "" + outId);
        Facility[] outFacs = facs.toArray(new Facility[0]);
        Country out = new Country(outName, outId, outFacs);

        return out;
    }

    public Country getCountry(String name)
    {
        List<Facility> facs = new ArrayList<>();
        String query = "SELECT id, name FROM countries WHERE name LIKE ? LIMIT 1";
        String outName = "";
        int outId = -1;
        String cleaned = name.trim().replace("'", "’").replaceFirst("(?i)^the\\s+", "");

        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(query))
        {
            s.setString(1, "%" + cleaned + "%");
            try (ResultSet rs = s.executeQuery())
            {
                if (rs.next()) // If valid query
                {
                    outName = rs.getString("name");
                    outId = rs.getInt("id");
                } 
                else 
                {
                    return null;
                }
            }

        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getStackTrace());
            return null;
        }

        facs = this.getRecords(columnNames[3], "" + outId);
        Facility[] outFacs = facs.toArray(new Facility[0]);
        Country out = new Country(outName, outId, outFacs);

        return out;
    }


    /**
     * Imports a file containing tab separated values into a database.
     * Does not check to see if file is formatted correctly
     * Returns the nuumber of records added to the database
     * NOTE: Assumes the existence of a header to skip
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

        // columnName values:
        // 0 - id, 1 - name, 2 - abbreviation, 3 - mission_id
        this.createTable();
        BufferedReader reader = new BufferedReader(new FileReader(pathToTSV));
        String readText;

        // Skip header
        reader.readLine();

        int numRecords = 0;
        while ((readText = reader.readLine()) != null)
        {
            // Each coloumn should correspond with the requisite value, apart from id
            String[] data = readText.split("\t");

            String name = data[0];
            String abbreviation = data[1];
            int country_id = -1;
            try { country_id = Integer.parseInt(data[2]); } catch (NumberFormatException e) { country_id = -1; };
            if (country_id <= 0 || country_id > 196) { reader.close(); return 0; }

            this.addRecord(name, abbreviation, country_id);
            numRecords++;
        }

        reader.close();
        return numRecords;
    }


    /**
     * Private helper functions below
     */
    

    @Override 
    public void createTable() 
    {
        // Create Country Table
        /*
         *  Country Table Schema
         *  CREATE TABLE countries
         *  (
         *  id INTEGER PRIMARY KEY,
         *  name TEXT NOT NULL
         *  )
        */
        String createSql = "CREATE TABLE IF NOT EXISTS countries (id INTEGER PRIMARY KEY, name TEXT NOT NULL)";
        String insertSql = "INSERT INTO countries (name) VALUES (?)";

        try (Connection c = connect(); Statement s = c.createStatement()) {
            // 1. Create the table FIRST
            s.execute(createSql);

            // 2. Now prepare the INSERT statement after the table exists
            try (PreparedStatement ps = c.prepareStatement(insertSql)) {
                for (Countries country : Countries.values()) {
                    ps.setString(1, country.getName());
                    ps.addBatch();
                }
                // 3. Execute the batch on ps, NOT s
                ps.executeBatch();
            }

            DebugHelper.debugPrintln("Country table created!");

        } catch (SQLException e) {
            DebugHelper.debugPrintln("FacilityManager: " + e.getMessage());
            DebugHelper.debugPrintln("Country table not created...");
        }

        // Create the Facilities table
        /*
         * Table Schema
         * CREATE TABLE facilities 
         * (
         *  id INTEGER PRIMARY KEY, 
         *  name TEXT NOT NULL, 
         *  abbreviation TEXT NOT NULL, 
         *  country_id INTEGER,
         *  mission_id INTEGER,
         *
         *  CONSTRAINT fk_country FOREIGN KEY (country_id) REFERENCES countries(id)
         *  )
         */
        try (Connection c = connect(); Statement s = c.createStatement()) {
 //           s.execute(
 //                   "CREATE TABLE IF NOT EXISTS %s (%s INTEGER PRIMARY KEY, %s TEXT NOT NULL, "
 //                   .formatted(this.tableName, this.columnNames[0], this.columnNames[1])
 //                   + "%s TEXT NOT NULL, %s INTEGER)".formatted(this.columnNames[2], this.columnNames[3])
 //                   );
            s.execute(
                    """
                    CREATE TABLE IF NOT EXISTS %s 
                    (
                     %s INTEGER PRIMARY KEY, 
                     %s TEXT NOT NULL,
                     %s TEXT NOT NULL,
                     %s INTEGER NOT NULL,
                     %s INTEGER,
                     
                     CONSTRAINT fk_country FOREIGN KEY (%s) REFERENCES countries(id)
                     )
                    """
                    .formatted
                    (
                        this.tableName,
                        this.columnNames[0], // id
                        this.columnNames[1], // name
                        this.columnNames[2], // abbreviation
                        this.columnNames[3], // country_id
                        this.columnNames[4], // mission_id
                        this.columnNames[3] // country_id fkey
                    )
                );
        } catch (SQLException e) {
            DebugHelper.debugPrintln(e.getMessage());
        }
    }
}
