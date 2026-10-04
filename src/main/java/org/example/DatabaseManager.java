package org.example;

import java.sql.*;

public class DatabaseManager
{
    Connection connection;
    String URL = "jdbc::sqlite:data/mission_briefs.tsv";

    public DatabaseManager()
    {
        
    }

    public DatabaseManager(String url)
    {
        this.setURL(url);
    }

    public void setURL(String url)
    {
        this.URL = url;
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

    public String getRecords(String header)
    {
        return null;
    }

    public String getColumns(String header)
    {
        return null;
    }

}
