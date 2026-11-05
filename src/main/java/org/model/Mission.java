package org.model;

public class Mission {
    private final int id;
    private final String title;
    private final int[] briefIds;
    private final int[] agentIds;
    private int facilityId;

    public Mission(int id, String title, int[] briefIds, int[] agentIds, int facilityId) {
        if (id <= 0) {
            throw new IllegalArgumentException("Mision ID must be positive");
        }

        this.id = id;
        this.title = title;
        this.briefIds = briefIds;
        this.agentIds = agentIds;
        setFacilityId(facilityId);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return this.title;
    }

    public int[] getBriefIds() {
        return this.briefIds;
    }

    public int[] getAgentIds() {
        return this.agentIds;
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
