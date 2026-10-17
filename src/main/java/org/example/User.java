package org.example;

import java.sql.Date;

public class User
{
    private AccessLevel level;
    private String name;
    private String dateOfBirth;
    private String dateOfDeath;
    private String notes;

    public User()
    {
        this.level = AccessLevel.Standard;
    }

    private User(String name, String dateOfBirth, 
                String dateOfDeath, String notes, 
                AccessLevel level)
    {
        this.setAccessLevel(level);
    }

    /**
     * Getters and Setters
     */

    public AccessLevel getAccessLevel()
    {
        return this.level;
    }

    public void setAccessLevel(AccessLevel level)
    {
        this.level = level;
    }

    public String getDateOfBirth()
    {
        return this.dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth)
    {
        this.dateOfBirth = dateOfBirth;
    }

    public String getDateOfDeath()
    {
        return this.dateOfDeath;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * End Getters and Setters
     */
}
