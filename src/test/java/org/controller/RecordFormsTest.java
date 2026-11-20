package org.controller;

import org.junit.jupiter.api.Test;
import org.model.RecordType;
import org.view.FormField;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordFormsTest {

    private static List<String> names(RecordType type) {
        return RecordForms.fieldsFor(type).stream().map(FormField::name).toList();
    }

    private static List<String> required(RecordType type) {
        return RecordForms.fieldsFor(type).stream().filter(FormField::required).map(FormField::name).toList();
    }

    @Test
    void briefFormHasTitleDateAndText() {
        assertEquals(List.of("title", "date", "text"), names(RecordType.BRIEF));
        assertEquals(List.of("title", "date", "text"), required(RecordType.BRIEF));
    }

    @Test
    void agentFormMakesDeathDateAndNotesOptional() {
        assertEquals(List.of("name", "dateOfBirth", "dateOfDeath", "notes", "facilityId"), names(RecordType.AGENT));
        assertEquals(List.of("name", "dateOfBirth", "facilityId"), required(RecordType.AGENT));
    }

    @Test
    void facilityFormHasNameAndAbbreviation() {
        assertEquals(List.of("name", "abbreviation", "countryId"), names(RecordType.FACILITY));
    }

    @Test
    void missionFormAllowsNoBriefsOrAgents() {
        assertEquals(List.of("title", "facilityId", "briefIds", "agentIds"), names(RecordType.MISSION));
        assertEquals(List.of("title", "facilityId"), required(RecordType.MISSION));
    }

    @Test
    void dateAndIdFieldsUseMatchingKinds() {
        assertEquals(FormField.Kind.DATE, RecordForms.fieldsFor(RecordType.BRIEF).get(1).kind());
        assertEquals(FormField.Kind.NUMBER, RecordForms.fieldsFor(RecordType.AGENT).get(4).kind());
        assertEquals(FormField.Kind.ID_LIST, RecordForms.fieldsFor(RecordType.MISSION).get(2).kind());
    }

    @Test
    void everyRecordTypeHasAForm() {
        for (RecordType type : RecordType.values()) {
            assertFalse(RecordForms.fieldsFor(type).isEmpty(), type.name());
        }
    }
}
