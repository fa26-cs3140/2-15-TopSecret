package org.example;

// TODO: Refactor this to specifically manage Users
public class UserManager implements IDatabaseUser
{
    private String sqlURL = "jdbc:sqlite:data/agency_info.db";
    private final String table = "agents";

    private final String username = "user";
    private final String password = "pwd";

    public UserManager()
    {
    }

    public UserManager(String url)
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

    public String[] getRecords(String url, String column, String value)
    {
        return null;
    }

    public String[] getColumn(String url, String column)
    {
        return null;
    }
}
