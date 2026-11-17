package org.controller;

import org.junit.jupiter.api.Test;
import org.model.RecordType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EditHandlerTest {

    @Test
    void constructorRejectsNullController() {
        assertThrows(IllegalArgumentException.class, () -> new EditHandler(null));
        assertDoesNotThrow(() -> new EditHandler(mock(AppController.class)));
    }

    @Test
    void parsesNewRecordPath() {
        EditHandler.EditPath path = EditHandler.parse("/briefs/new");

        assertEquals(RecordType.BRIEF, path.type());
        assertEquals("briefs", path.segment());
        assertNull(path.id());
    }

    @Test
    void parsesEditPathWithId() {
        EditHandler.EditPath path = EditHandler.parse("/missions/12/edit");

        assertEquals(RecordType.MISSION, path.type());
        assertEquals("missions", path.segment());
        assertEquals(12, path.id());
    }

    @Test
    void parsesEveryRecordTypeSegment() {
        assertEquals(RecordType.AGENT, EditHandler.parse("/agents/new").type());
        assertEquals(RecordType.FACILITY, EditHandler.parse("/facilities/3/edit").type());
    }

    @Test
    void ignoresPathsThatAreNotEditUrls() {
        for (String path : new String[]{null, "", "briefs/new", "/briefs", "/briefs/1", "/briefs/new/x",
                "/briefs/abc/edit", "/briefs/1/edit/x", "/briefs/-1/edit", "/users/new", "/audit/new",
                "/briefs/9999999999/edit"}) {
            assertNull(EditHandler.parse(path), String.valueOf(path));
        }
    }
}
