package org.model;

public class Country
{
    private Facility[] facilities;
    private String name;
    private int id;

    public Country(String name, int id, Facility[] facilities)
    {
        this.setFacilities(facilities);
        this.setID(id);
        this.setName(name);
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank or null");
        }

        this.name = name;
    }

    public Facility[] getFacilities()
    {
        return this.facilities;
    }

    public void setFacilities(Facility[] facilities)
    {
        this.facilities = facilities;
    }

    private void setID(int id)
    {
        if (id <= 0) {
            throw new IllegalArgumentException("ID out of range");
        }

        this.id = id;
    }

    public int getID()
    {
        return this.id;
    }


}
