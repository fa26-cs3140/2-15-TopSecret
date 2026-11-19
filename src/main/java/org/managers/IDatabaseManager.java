package org.managers;

import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

public abstract class IDatabaseManager<T>
{
    // Fields
    public String url;

     // Fields that must be included
    public abstract String getURL();

    public abstract String getTableName();

    public abstract String[] getColumnNames();

    // Funcations that must be included
    public abstract List<T> getRecords(String column, String key);

    public abstract T addRecord(T value);

    public abstract Boolean updateRecord(String checkColumn, String key, String changeColumn, String newVal);

    public abstract int importTSV(String pathToTSV, String tableName, String dirOut, String dbOutName) throws IOException;

    public abstract void createTable();

    // General public defined functions

    /**
     * Returns true if a table matching the name table exists at this.url, false otherwise
     */
    public Boolean tableExists(String table)
    {
        try (Connection c = connect();)
        {
            DatabaseMetaData meta = c.getMetaData();

            try (ResultSet rs = meta.getTables(null, null, table, new String [] {"TABLE"})) {
                return rs.next();
            }

        }
        catch (SQLException e) { return false; }
    }

    // Predefined helper functions for these bad boys to use
    protected Connection connect() throws SQLException 
    {
        Connection c = DriverManager.getConnection(this.url);
        c.createStatement().execute("PRAGMA foreign_keys = ON");
        return c;
    }

    /**
     * NOTE ON insert() and update():
     * These two are for now helper functions to facilitate interacting with the database.
     * Technically they do the same thing, but insert() returns the id of the new item
     * where update() returns the number of changes made by the sql operation.
     *
     * For now these should be used for their respective insertRecord() and updateRecord() functions
     * despite their slightly odd implementation. This can be fixed later. - Zaid
     */

    /**
     * Inserts a record into a database by taking in an sql query.
     * TODO: Generalise this so that each class can define its own method of insertion
     * without taking in raw SQL
     *
     * Returns the primary key value of the added record
     */
    protected int insert(String sql, Object... values) 
    {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            ps.executeUpdate();
            try (ResultSet rs = c.createStatement().executeQuery("SELECT last_insert_rowid()")) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalArgumentException("The database rejected the record: " + e.getMessage());
        }
    }

    /**
     * Updates a record into a database by taking in an sql query.
     * TODO: Generalise this so that each class can define its own method of insertion
     * without taking in raw SQL
     *
     * Returns the number of changes made to the database
     */
    protected int update(String sql, Object... values)
    {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            return ps.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new IllegalArgumentException("The database rejected the record: " + e.getMessage());
        }
    }

    /**
     * Shared body of updateRecord: sets changeColumn to value on rows where checkColumn LIKE key.
     * Values are bound as parameters (a quote in user input cannot break the query); column
     * names cannot be bound, so they must be one of getColumnNames(). A null value stores NULL.
     *
     * @return true when at least one row changed
     */
    protected Boolean updateColumn(String checkColumn, String key, String changeColumn, String value)
    {
        List<String> columns = Arrays.asList(getColumnNames());
        if (!columns.contains(checkColumn) || !columns.contains(changeColumn)) {
            throw new IllegalArgumentException("Unknown column for " + getTableName() + ".");
        }
        return update("UPDATE " + getTableName() + " SET " + changeColumn + " = ? WHERE " + checkColumn + " LIKE ?",
                      value, key) > 0;
    }
}
