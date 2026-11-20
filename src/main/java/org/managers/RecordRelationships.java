package org.managers;

import org.model.RecordType;
import org.model.RelatedRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Resolves the relationships already represented by the agency managers. */
public final class RecordRelationships {

    private final BriefManager briefs;
    private final AgentManager agents;
    private final FacilityManager facilities;
    private final MissionManager missions;

    public RecordRelationships(BriefManager briefs, AgentManager agents,
                               FacilityManager facilities, MissionManager missions) {
        this.briefs = briefs;
        this.agents = agents;
        this.facilities = facilities;
        this.missions = missions;
    }

    public List<RelatedRecord> find(RecordType type, int id) {
        List<RelatedRecord> related = new ArrayList<>();
        switch (type) {
            case BRIEF -> missions.getRecords("id", "%").stream()
                    .filter(mission -> contains(mission.getBriefIds(), id))
                    .forEach(mission -> related.add(new RelatedRecord(
                            RecordType.MISSION, mission.getId(), mission.getTitle())));
            case AGENT -> {
                agents.getRecords("id", String.valueOf(id)).forEach(agent ->
                        addFacility(related, agent.getFacilityId()));
                missions.getRecords("id", "%").stream()
                        .filter(mission -> contains(mission.getAgentIds(), id))
                        .forEach(mission -> related.add(new RelatedRecord(
                                RecordType.MISSION, mission.getId(), mission.getTitle())));
            }
            case COUNTRY -> facilities.getRecords("country_id", String.valueOf(id)).forEach(facility ->
                    related.add(new RelatedRecord(RecordType.FACILITY, facility.getId(), facility.getName())));
            case FACILITY -> {
                facilities.getRecords("id", String.valueOf(id)).forEach(facility -> {
                    var country = facilities.getCountry(facility.getCountryID());
                    if (country != null) {
                        related.add(new RelatedRecord(RecordType.COUNTRY, country.getID(), country.getName()));
                    }
                });
                agents.getRecords("facilityId", String.valueOf(id)).forEach(agent ->
                        related.add(new RelatedRecord(RecordType.AGENT, agent.getId(), agent.getName())));
                missions.getRecords("facilityId", String.valueOf(id)).forEach(mission ->
                        related.add(new RelatedRecord(RecordType.MISSION, mission.getId(), mission.getTitle())));
            }
            case MISSION -> missions.getRecords("id", String.valueOf(id)).forEach(mission -> {
                addFacility(related, mission.getFacilityId());
                briefs.getRecords("id", "%").stream()
                        .filter(brief -> contains(mission.getBriefIds(), brief.getId()))
                        .forEach(brief -> related.add(new RelatedRecord(
                                RecordType.BRIEF, brief.getId(), brief.getTitle())));
                agents.getRecords("id", "%").stream()
                        .filter(agent -> contains(mission.getAgentIds(), agent.getId()))
                        .forEach(agent -> related.add(new RelatedRecord(
                                RecordType.AGENT, agent.getId(), agent.getName())));
            });
        }
        return List.copyOf(related);
    }

    private void addFacility(List<RelatedRecord> related, int id) {
        facilities.getRecords("id", String.valueOf(id)).forEach(facility ->
                related.add(new RelatedRecord(RecordType.FACILITY, facility.getId(), facility.getName())));
    }

    private static boolean contains(int[] ids, int id) {
        return Arrays.stream(ids).anyMatch(candidate -> candidate == id);
    }
}
