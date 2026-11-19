package org.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormFieldTest {

    @Test
    void keepsNameLabelKindAndRequired() {
        FormField field = new FormField("title", "Title", FormField.Kind.TEXT, true);

        assertEquals("title", field.name());
        assertEquals("Title", field.label());
        assertEquals(FormField.Kind.TEXT, field.kind());
        assertTrue(field.required());
    }

    @Test
    void rejectsMissingNameLabelOrKind() {
        assertThrows(IllegalArgumentException.class, () -> new FormField(null, "Title", FormField.Kind.TEXT, true));
        assertThrows(IllegalArgumentException.class, () -> new FormField(" ", "Title", FormField.Kind.TEXT, true));
        assertThrows(IllegalArgumentException.class, () -> new FormField("title", null, FormField.Kind.TEXT, true));
        assertThrows(IllegalArgumentException.class, () -> new FormField("title", "", FormField.Kind.TEXT, true));
        assertThrows(IllegalArgumentException.class, () -> new FormField("title", "Title", null, true));
    }
}
