package org.example;

import org.model.Agent;
import org.model.Brief;
import org.model.Facility;

import java.util.List;

// Add methods throw IllegalArgumentException for invalid data.
public interface AgencyDataService {

    List<Brief> getBriefs();

    List<Agent> getAgents();

    List<Facility> getFacilities();

    Brief addBrief(String title, String date, String text);

    Agent addAgent(String name, String dateOfBirth, String dateOfDeath, String notes, int facilityId);

    Facility addFacility(String name, String abbreviation);
}
