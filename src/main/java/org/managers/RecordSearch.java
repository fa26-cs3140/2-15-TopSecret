package org.managers;

import org.model.RecordType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Searches the text fields of agency records for the terminal search workflow. */
public final class RecordSearch {

    public record Match(RecordType type, int id, String title) {
    }

    private final BriefManager briefs;
    private final AgentManager agents;
    private final FacilityManager facilities;
    private final MissionManager missions;

    public RecordSearch(BriefManager briefs, AgentManager agents, FacilityManager facilities, MissionManager missions) {
        this.briefs = Objects.requireNonNull(briefs);
        this.agents = Objects.requireNonNull(agents);
        this.facilities = Objects.requireNonNull(facilities);
        this.missions = Objects.requireNonNull(missions);
    }

    public List<Match> find(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String term = query.strip().toLowerCase(Locale.ROOT);
        List<Match> results = new ArrayList<>();
        for (var brief : briefs.getRecords("id", "%")) {
            if (contains(term, brief.getTitle(), brief.getDate(), brief.getText())) {
                results.add(new Match(RecordType.BRIEF, brief.getId(), brief.getTitle()));
            }
        }
        for (var agent : agents.getRecords("id", "%")) {
            if (contains(term, agent.getName(), agent.getDateOfBirth(), agent.getDateOfDeath(), agent.getNotes())) {
                results.add(new Match(RecordType.AGENT, agent.getId(), agent.getName()));
            }
        }
        for (var facility : facilities.getRecords("id", "%")) {
            if (contains(term, facility.getName(), facility.getAbbreviation())) {
                results.add(new Match(RecordType.FACILITY, facility.getId(), facility.getName()));
            }
        }
        for (var mission : missions.getRecords("id", "%")) {
            if (contains(term, mission.getTitle())) {
                results.add(new Match(RecordType.MISSION, mission.getId(), mission.getTitle()));
            }
        }
        for (var country : facilities.getCountries()) {
            if (contains(term, country.getName())) {
                results.add(new Match(RecordType.COUNTRY, country.getID(), country.getName()));
            }
        }
        return List.copyOf(results);
    }

    private static boolean contains(String term, String... fields) {
        for (String field : fields) {
            if (field != null && field.toLowerCase(Locale.ROOT).contains(term)) {
                return true;
            }
        }
        return false;
    }
}
