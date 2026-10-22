package org.example;

import org.example.model.Agent;
import org.example.model.Brief;
import org.example.model.Facility;

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
