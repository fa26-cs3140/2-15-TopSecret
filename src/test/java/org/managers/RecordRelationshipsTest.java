package org.managers;

import org.junit.jupiter.api.Test;
import org.model.Agent;
import org.model.Brief;
import org.model.Facility;
import org.model.Mission;
import org.model.RecordType;
import org.model.RelatedRecord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RecordRelationshipsTest {

    @Test
    void resolvesAllDirectionsAndFiltersUnassignedItems() {
        BriefManager briefs = mock(BriefManager.class);
        AgentManager agents = mock(AgentManager.class);
        FacilityManager facilities = mock(FacilityManager.class);
        MissionManager missions = mock(MissionManager.class);
        var agent = new Agent(2, "Ghost", "1990-01-01", null, null, 3);
        var mission = new Mission(4, "Recon", new int[]{1}, new int[]{2}, 3);
        when(agents.getRecords("id", "2")).thenReturn(List.of(agent));
        when(agents.getRecords("id", "%")).thenReturn(List.of(agent));
        when(agents.getRecords("facilityId", "3")).thenReturn(List.of(agent));
        when(missions.getRecords("id", "%")).thenReturn(List.of(mission));
        when(missions.getRecords("id", "4")).thenReturn(List.of(mission));
        when(missions.getRecords("facilityId", "3")).thenReturn(List.of(mission));
        when(briefs.getRecords("id", "%")).thenReturn(List.of(
                new Brief(1, "Notes", "2020-01-01", "text", null),
                new Brief(9, "Unassigned", "2020-01-01", "text", null)));
        when(facilities.getCountry(1)).thenReturn(new org.model.Country("Afghanistan", 1, new Facility[0]));
        when(facilities.getRecords("country_id", "1")).thenReturn(List.of(new Facility(3, "HQ", "HQ", 1)));
        when(facilities.getRecords("id", "3")).thenReturn(List.of(new Facility(3, "HQ", "HQ", 1)));
        RecordRelationships relationships = new RecordRelationships(briefs, agents, facilities, missions);
        RelatedRecord facilityLink = new RelatedRecord(RecordType.FACILITY, 3, "HQ");
        RelatedRecord agentLink = new RelatedRecord(RecordType.AGENT, 2, "Ghost");
        RelatedRecord missionLink = new RelatedRecord(RecordType.MISSION, 4, "Recon");

        assertEquals(List.of(missionLink), relationships.find(RecordType.BRIEF, 1));
        assertEquals(List.of(facilityLink, missionLink), relationships.find(RecordType.AGENT, 2));
        assertEquals(List.of(new RelatedRecord(RecordType.COUNTRY, 1, "Afghanistan"), agentLink, missionLink),
                relationships.find(RecordType.FACILITY, 3));
        assertEquals(List.of(facilityLink), relationships.find(RecordType.COUNTRY, 1));
        assertEquals(List.of(facilityLink, new RelatedRecord(RecordType.BRIEF, 1, "Notes"), agentLink),
                relationships.find(RecordType.MISSION, 4));
        assertEquals(List.of(), relationships.find(RecordType.BRIEF, 9));
    }
}
