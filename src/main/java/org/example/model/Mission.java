package org.example.model;

public class Mission {
    private final int id;
    private int facilityId;

    public Mission(int id, int facilityId) {
        if (id <= 0) {
            throw new IllegalArgumentException("Mision ID must be positive");
        }

        this.id = id;
        setFacilityId(facilityId);
    }

    public int getId() {
        return id;
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
}
