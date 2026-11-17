package org.model;

public class Country
{
    private Facility[] facilities;
    private String name;

    public Country(Facility[] facilities)
    {
        this.facilities = facilities;
    }

    public String getName()
    {
        return this.name;
    }

    public Facility[] getFacilities()
    {
        return this.facilities;
    }

    public void setFacilities(Facility[] facilities)
    {
        this.facilities = facilities;
    }

    public void addFacility(Facility facility)
    {
        // Round about way to allocate some more memory for this
        Facility[] temp = new Facility[this.facilities.length + 1];
        for (int i = 0; i < this.facilities.length; i++) temp[i] = facilities[i];
        temp[facilities.length + 1] = facility;
        this.facilities = temp;
    }
}
