package org.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.model.Agent;
import org.model.Brief;
import org.model.Facility;
import org.model.Mission;
import org.model.RecordType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecordSearchTest {

    private final BriefManager briefs = mock(BriefManager.class);
    private final AgentManager agents = mock(AgentManager.class);
    private final FacilityManager facilities = mock(FacilityManager.class);
    private final MissionManager missions = mock(MissionManager.class);
    private final RecordSearch search = new RecordSearch(briefs, agents, facilities, missions);

    @BeforeEach
    void setUp() {
        when(briefs.getRecords("id", "%")).thenReturn(List.of(
                new Brief(1, "Briefing", "2024-01-02", "O'Brien escaped; success 100%. Briefing complete.", null)));
        when(agents.getRecords("id", "%")).thenReturn(List.of(
                new Agent(2, "Mira", "1961-03-04", "2004-07-06", "Alias: nightbird", 3),
                new Agent(5, "Robin", "1975-01-01", null, null, 3)));
        when(facilities.getCountries()).thenReturn(List.of(new org.model.Country("Test islands", 301,
                new Facility[0])));
        when(facilities.getRecords("id", "%")).thenReturn(List.of(new Facility(3, "Headquarters", "HQ")));
        when(missions.getRecords("id", "%")).thenReturn(List.of(
                new Mission(4, "Sandstorm", new int[]{1}, new int[]{2}, 3)));
    }

    @ParameterizedTest
    @CsvSource({
            "' BRIEFING ', BRIEF, 1, Briefing",
            "2024-01-02, BRIEF, 1, Briefing",
            "O'Brien, BRIEF, 1, Briefing",
            "100%, BRIEF, 1, Briefing",
            "Mira, AGENT, 2, Mira",
            "1961-03-04, AGENT, 2, Mira",
            "2004-07-06, AGENT, 2, Mira",
            "nightbird, AGENT, 2, Mira",
            "Headquarters, FACILITY, 3, Headquarters",
            "hq, FACILITY, 3, Headquarters",
            "Sandstorm, MISSION, 4, Sandstorm",
            "islands, COUNTRY, 301, Test islands"
    })
    void searchesEveryCurrentTextFieldAndReturnsEachRecordOnce(
            String query, RecordType type, int id, String title) {
        assertEquals(List.of(new RecordSearch.Match(type, id, title)), search.find(query));
    }

    @ParameterizedTest
    @CsvSource({"'   '", "'not found'", "'%missing%'","''"})
    void blankOrUnmatchedQueryHasNoResults(String query) {
        assertEquals(List.of(), search.find(query));
    }
}
