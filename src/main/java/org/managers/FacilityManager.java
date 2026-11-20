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
        createTable();
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
                            rs.getString(this.columnNames[2])
                            ));
                int countryId = rs.getInt("country_id");
                if (!rs.wasNull()) {
                    facilities.getLast().setCountry(countryId);
                }
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
                             name, abbreviation, country == 0 ? null : country
                );
        return country == 0 ? new Facility(id, name, abbreviation) : new Facility(id, name, abbreviation, country);
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

    public Facility addRecord(String name, String abbreviation) {
        return addRecord(new Facility(1, name, abbreviation));
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

    public Country getCountry(Countries country) {
        return getCountry(country.ordinal() + 1);
    }

    public Country getCountry(int countryId) {
        return getCountries().stream().filter(country -> country.getID() == countryId).findFirst().orElse(null);
    }

    public Country getCountry(String name) {
        if (name == null) {
            return null;
        }
        return getCountries().stream()
                .filter(country -> country.getName().regionMatches(true, 0, name, 0, name.length()))
                .findFirst().orElse(null);
    }

    public List<Country> getCountries() {
        List<Country> countries = new ArrayList<>();
        List<Facility> facilities = getRecords("id", "%");
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("SELECT id, name FROM countries ORDER BY id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                int id = result.getInt("id");
                Facility[] assigned = facilities.stream().filter(facility -> facility.getCountryID() == id)
                        .toArray(Facility[]::new);
                countries.add(new Country(result.getString("name"), id, assigned));
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("Unable to read countries.", failure);
        }
        return countries;
    }

    public Country addCountry(String name) {
        Country validated = new Country(name, 1, new Facility[0]);
        int id = insert("INSERT INTO countries (name) VALUES (?)", validated.getName());
        return new Country(name, id, new Facility[0]);
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
            String[] data = readText.replace("\\t", "\t").split("\t");

            String name = data[0];
            String abbreviation = data[1];
            if (data.length > 2) {
                int countryId = Integer.parseInt(data[2]);
                this.addRecord(name, abbreviation, countryId);
            } else {
                this.addRecord(name, abbreviation);
            }
            numRecords++;
        }

        reader.close();
        return numRecords;
    }


    /**
     * Private helper functions below
     */
    

    @Override
    public void createTable() {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS countries (id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
                try (PreparedStatement seed = connection.prepareStatement(
                        "INSERT OR IGNORE INTO countries (id, name) VALUES (?, ?)")) {
                    for (Countries country : Countries.values()) {
                        seed.setInt(1, country.ordinal() + 1);
                        seed.setString(2, country.getName());
                        seed.addBatch();
                    }
                    seed.executeBatch();
                }
                statement.execute("CREATE TABLE IF NOT EXISTS facilities ("
                        + "id INTEGER PRIMARY KEY, name TEXT NOT NULL, abbreviation TEXT NOT NULL, "
                        + "country_id INTEGER REFERENCES countries(id), mission_id INTEGER)");
                boolean hasCountry = false;
                try (ResultSet columns = statement.executeQuery("PRAGMA table_info(facilities)")) {
                    while (columns.next()) {
                        hasCountry |= columns.getString("name").equals("country_id");
                    }
                }
                if (!hasCountry) {
                    statement.execute("ALTER TABLE facilities ADD COLUMN country_id INTEGER REFERENCES countries(id)");
                }
                connection.commit();
            } catch (SQLException failure) {
                connection.rollback();
                throw failure;
            }
        } catch (SQLException failure) {
            throw new IllegalStateException("Unable to initialize facilities and countries.", failure);
        }
    }
}
