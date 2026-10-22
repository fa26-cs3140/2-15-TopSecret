package org.example.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BriefTest {
    @Test
    void testConstructorAndGetters() {
        Brief brief = new Brief(1, "Operation Sandtrap", "1970-11-03", "Secret intelligence report", 2);

        assertEquals(1, brief.getId());
        assertEquals("Operation Sandtrap", brief.getTitle());
        assertEquals("1970-11-03", brief.getDate());
        assertEquals("Secret intelligence report", brief.getText());
        assertEquals(2, brief.getMissionId());
    }

    @Test
    void testSetters() {
        Brief brief = new Brief(1, "Operation Sandtrap", "1970-11-03", "Original text", 2);

        brief.setTitle("Operation Nightfall");
        brief.setDate("1980-05-15");
        brief.setText("Updated intelligence report");
        brief.setMissionId(3);

        assertEquals("Operation Nightfall", brief.getTitle());
        assertEquals("1980-05-15", brief.getDate());
        assertEquals("Updated intelligence report", brief.getText());
        assertEquals(3, brief.getMissionId());

        // ID should stay the same
        assertEquals(1, brief.getId());
    }

    @Test
    void testInvalidId() {
        assertThrows(IllegalArgumentException.class, () -> new Brief(0, "Operation Sandtrap", "1970-11-03", "Secret report", 2));
        assertThrows(IllegalArgumentException.class, () -> new Brief(-1, "Operation Sandtrap", "1970-11-03", "Secret report", 2));
    }

    @Test
    void testInvalidTitle() {
        assertThrows(IllegalArgumentException.class, () -> new Brief(1, null, "1970-11-03", "Secret report", 2));
        assertThrows(IllegalArgumentException.class, () -> new Brief(1, "", "1970-11-03", "Secret report", 2));
        assertThrows(IllegalArgumentException.class, () -> new Brief(1, "   ", "1970-11-03", "Secret report", 2));
    }

    @Test
    void testInvalidTitleSetter() {
        Brief brief = new Brief(1, "Operation Sandtrap", "1970-11-03", "Secret report", 2);

        assertThrows(IllegalArgumentException.class, () -> brief.setTitle(null));
        assertThrows(IllegalArgumentException.class, () -> brief.setTitle(""));
        assertThrows(IllegalArgumentException.class, () -> brief.setTitle("   "));

        // Invalid changes should not modify the title
        assertEquals("Operation Sandtrap", brief.getTitle());
    }

    @Test
    void testInvalidDate() {
        Brief brief = new Brief(1, "Operation Sandtrap", "1970-11-03", "Secret report", 2);

        String[] invalidDates = {"1970-02-30", "1970-13-01", "1970-11-3", "November 3, 1970", "banana", ""};
        for (String date : invalidDates) {
            assertThrows(IllegalArgumentException.class, () -> brief.setDate(date));
        }

        assertThrows(IllegalArgumentException.class, () -> brief.setDate(null));

        // Original value should stay the same
        assertEquals("1970-11-03", brief.getDate());
    }

    @Test
    void testConstructorRejectsInvalidDate() {
        assertThrows(IllegalArgumentException.class, () -> new Brief(1, "Operation Sandtrap", null, "Secret report", 2));
        assertThrows(IllegalArgumentException.class, () -> new Brief(1, "Operation Sandtrap", "1970-02-30", "Secret report", 2));
    }

    @Test
    void testInvalidText() {
        assertThrows(IllegalArgumentException.class, () -> new Brief(1, "Operation Sandtrap", "1970-11-03", null, 2));

        Brief brief = new Brief(1, "Operation Sandtrap", "1970-11-03", "Original text", 2);

        assertThrows(IllegalArgumentException.class, () -> brief.setText(null));
        assertEquals("Original text", brief.getText());
    }

    @Test
    void testEmptyTextAllowed() {
        Brief brief = new Brief(1, "Operation Sandtrap", "1970-11-03", "", 2);

        assertEquals("", brief.getText());

        brief.setText("New report");
        assertEquals("New report", brief.getText());

        brief.setText("");
        assertEquals("", brief.getText());
    }

    @Test
    void testNullableMissionId() {
        Brief brief = new Brief(1, "Operation Sandtrap", "1970-11-03", "Secret report", null);

        assertNull(brief.getMissionId());

        brief.setMissionId(5);
        assertEquals(5, brief.getMissionId());

        brief.setMissionId(null);
        assertNull(brief.getMissionId());
    }

    @Test
    void testInvalidMissionId() {
        assertThrows(IllegalArgumentException.class, () -> new Brief(1, "Operation Sandtrap", "1970-11-03", "Secret report", 0));

        Brief brief = new Brief(1, "Operation Sandtrap", "1970-11-03", "Secret report", 2);

        assertThrows(IllegalArgumentException.class, () -> brief.setMissionId(0));
        assertThrows(IllegalArgumentException.class, () -> brief.setMissionId(-1));

        // Invalid changes should not modify the Mission ID
        assertEquals(2, brief.getMissionId());
    }
}
