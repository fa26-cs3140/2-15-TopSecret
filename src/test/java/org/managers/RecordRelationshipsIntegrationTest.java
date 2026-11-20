package org.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.model.RecordType;
import org.model.RelatedRecord;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordRelationshipsIntegrationTest {

    @TempDir
    Path directory;

    private RecordRelationships relationships;
    private RelatedRecord brief;
    private RelatedRecord agent;
    private RelatedRecord facility;
    private RelatedRecord mission;
    private int unassignedFacilityId;

    @BeforeEach
    void setUp() {
        String url = "jdbc:sqlite:" + directory.resolve("records.db");
        FacilityManager facilities = new FacilityManager(url);
        AgentManager agents = new AgentManager(url);
        BriefManager briefs = new BriefManager(url);
        MissionManager missions = new MissionManager(url);

        facilities.createTable();
        agents.createTable();
        briefs.createTable();
        missions.createTable();

        int facilityId = facilities.addRecord("HQ <West>", "HQ").getId();
        int agentId = agents.addRecord("Ghost", "1990-01-01", null, null, facilityId).getId();
        int briefId = briefs.addRecord("Field notes", "2024-01-02", "Brief text").getId();
        int missionId = missions.addRecord("Recon", new int[]{briefId}, new int[]{agentId}, facilityId).getId();
        unassignedFacilityId = facilities.addRecord("Outpost", "OUT").getId();

        facility = new RelatedRecord(RecordType.FACILITY, facilityId, "HQ <West>");
        agent = new RelatedRecord(RecordType.AGENT, agentId, "Ghost");
        brief = new RelatedRecord(RecordType.BRIEF, briefId, "Field notes");
        mission = new RelatedRecord(RecordType.MISSION, missionId, "Recon");
        relationships = new RecordRelationships(briefs, agents, facilities, missions);
    }

    @Test
    void resolvesLinksOnEachSideOfTheStoredRelationships() {
        assertEquals(List.of(agent, mission), relationships.find(RecordType.FACILITY, facility.id()));
        assertEquals(List.of(facility, mission), relationships.find(RecordType.AGENT, agent.id()));
        assertEquals(List.of(facility, brief, agent), relationships.find(RecordType.MISSION, mission.id()));
        assertEquals(List.of(mission), relationships.find(RecordType.BRIEF, brief.id()));
    }

    @Test
    void unassignedFacilityHasNoRelatedRecords() {
        assertEquals(List.of(), relationships.find(RecordType.FACILITY, unassignedFacilityId));
    }
}
