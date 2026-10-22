package org.example.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class Agent {
    private final int id;
    private String name;
    private String dateOfBirth;
    private String dateOfDeath;
    private String notes;
    private int facilityId;

    public Agent(int id, String name, String dateOfBirth, String dateOfDeath, String notes, int facilityId) {
        if (id <= 0) {
            throw new IllegalArgumentException("Agent ID must be positive");
        }

        this.id = id;

        setName(name);
        setDateOfBirth(dateOfBirth);
        setDateOfDeath(dateOfDeath);
        setNotes(notes);
        setFacilityId(facilityId);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Agent name cannot be null or blank.");
        }

        this.name = name;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        LocalDate birth = parseDate(dateOfBirth);

        // Check if DOB < DOD
        if (dateOfDeath != null) {
            LocalDate death = parseDate(dateOfDeath);

            if (birth.isAfter(death)) {
                throw new IllegalArgumentException("Date of birth cannot be after date of death.");
            }
        }

        this.dateOfBirth = dateOfBirth;
    }

    public String getDateOfDeath() {
        return dateOfDeath;
    }

    public void setDateOfDeath(String dateOfDeath) {
        if (dateOfDeath != null) {
            LocalDate death = parseDate(dateOfDeath);
            LocalDate birth = parseDate(dateOfBirth);

            if (death.isBefore(birth)) {
                throw new IllegalArgumentException("Date of death cannot be before date of birth.");
            }
        }

        this.dateOfDeath = dateOfDeath;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(int facilityId) {
        if (facilityId <= 0) {
            throw new IllegalArgumentException("Facility ID must be positive");
        }

        this.facilityId = facilityId;
    }

    // Internal helper method
    private LocalDate parseDate(String date) {
        // Check if date follows format yyyy-mm-dd
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Date must follow yyyy-MM-dd format.");
        }

        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + date, e);
        }
    }
}
