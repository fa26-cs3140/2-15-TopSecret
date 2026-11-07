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
        if (facilityId <= 0) {
            throw new IllegalArgumentException("Facility ID msut be positive");
        }

        this.id = id;
        this.title = title;
        this.briefIds = briefIds;
        this.agentIds = agentIds;
        this.facilityId = facilityId;
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

    public String getBriefIdsAsString() {
        String out = "[ ";
        for (int i = 0; i < this.briefIds.length; i++)
        {
            out += this.briefIds[i];
            if (i < this.briefIds.length - 1) out += ", ";
        }
        return out + " ]";
    }

    public String getAgentIdsAsString() {
        String out = "[ ";
        for (int i = 0; i < this.agentIds.length; i++)
        {
            out += this.agentIds[i];
            if (i < this.agentIds.length - 1) out += ", ";
        }
        return out + " ]";
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
