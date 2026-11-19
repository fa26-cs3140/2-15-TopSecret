package org.controller;

import org.model.RecordType;
import org.view.FormField;
import org.view.FormField.Kind;

import java.util.List;

// The fields shown on each record type's create/edit form, in display order.
// Field names are the keys used in form submissions and in AppController's value maps.
public final class RecordForms {

    private RecordForms() {
        // Utility class -> not instantiated
    }

    public static List<FormField> fieldsFor(RecordType type) {
        return switch (type) {
            case BRIEF -> List.of(
                    new FormField("title", "Title", Kind.TEXT, true),
                    new FormField("date", "Date (yyyy-mm-dd)", Kind.DATE, true),
                    new FormField("text", "Text", Kind.TEXTAREA, true));
            case AGENT -> List.of(
                    new FormField("name", "Name", Kind.TEXT, true),
                    new FormField("dateOfBirth", "Date of birth", Kind.DATE, true),
                    new FormField("dateOfDeath", "Date of death", Kind.DATE, false),
                    new FormField("notes", "Notes", Kind.TEXTAREA, false),
                    new FormField("facilityId", "Facility ID", Kind.NUMBER, true));
            // TODO (week 2): add a country field once Team A adds the Country type
            case FACILITY -> List.of(
                    new FormField("name", "Name", Kind.TEXT, true),
                    new FormField("abbreviation", "Abbreviation", Kind.TEXT, true));
            case MISSION -> List.of(
                    new FormField("title", "Title", Kind.TEXT, true),
                    new FormField("facilityId", "Facility ID", Kind.NUMBER, true),
                    new FormField("briefIds", "Brief IDs (comma separated)", Kind.ID_LIST, false),
                    new FormField("agentIds", "Agent IDs (comma separated)", Kind.ID_LIST, false));
        };
    }
}
