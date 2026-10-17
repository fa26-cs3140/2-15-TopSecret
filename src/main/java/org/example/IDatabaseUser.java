package org.example;

public interface IDatabaseUser
{
    public void setURL(String url);

    public String getURL();

    public String[] getRecords(String url, String column, String value);
    
    public String[] getColumn(String url, String column);

}
