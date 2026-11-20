package org.model;

public class Facility {
    private int id;
    private String name;
    private String abbreviation;
    private int countryId;

    /** Existing facilities may remain unassigned until a country is selected. */
    public Facility(int id, String name, String abbreviation) {
        if (id <= 0) {
            throw new IllegalArgumentException("Facility ID must be positive.");
        }

        this.id = id;
        this.setName(name);
        this.setAbbreviation(abbreviation);
    }

    public Facility(int id, String name, String abbreviation, int country) {
        this(id, name, abbreviation);
        setCountry(country);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Facility name cannot be null or blank.");
        }

        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        if (abbreviation == null || abbreviation.isBlank()) {
            throw new IllegalArgumentException("Facility abbreviation cannot be null or blank.");
        }

        this.abbreviation = abbreviation;
    }

    public int getCountryID() {
        return this.countryId;
    }

    public void setCountry(int countryId) {
        if (countryId <= 0) {
            throw new IllegalArgumentException("Country cannot be null");
        }

        this.countryId = countryId;
    }
}
