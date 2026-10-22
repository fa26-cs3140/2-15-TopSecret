package org.example.model;

public class Facility {
    private int id;
    private String name;
    private String abbreviation;

    public Facility(int id, String name, String abbreviation) {
        if (id < 0) {
            throw new IllegalArgumentException("Facility ID must be positive.");
        }

        this.id = id;
        setName(name);
        setAbbreviation(abbreviation);
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
}