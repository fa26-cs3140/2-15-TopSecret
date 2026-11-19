package org.view;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormViewTest {

    private static final List<FormField> FIELDS = List.of(
            new FormField("title", "Title", FormField.Kind.TEXT, true),
            new FormField("date", "Date", FormField.Kind.DATE, true),
            new FormField("text", "Text", FormField.Kind.TEXTAREA, false));

    @Test
    void newFormPostsToActionWithEmptyInputs() {
        String html = FormView.form("/briefs/new", FIELDS, Map.of(), null);

        assertTrue(html.contains("<form method=\"post\" action=\"/briefs/new\">"));
        assertTrue(html.contains("name=\"title\""));
        assertTrue(html.contains("name=\"date\""));
        assertTrue(html.contains("<textarea"));
        assertFalse(html.contains("value=\"x"));
    }

    @Test
    void editFormIsFilledWithEscapedValues() {
        String html = FormView.form("/briefs/1/edit", FIELDS,
                Map.of("title", "Op <Sandtrap> \"1\"", "text", "Bug & listen"), null);

        assertTrue(html.contains("value=\"Op &lt;Sandtrap&gt; &quot;1&quot;\""));
        assertTrue(html.contains(">Bug &amp; listen</textarea>"));
    }

    @Test
    void labelsEveryInputAndMarksRequiredFields() {
        String html = FormView.form("/briefs/new", FIELDS, Map.of(), null);

        assertTrue(html.contains("<label for=\"title\">Title</label>"));
        assertTrue(html.contains("type=\"date\""));
        assertEquals(2, html.split("required").length - 1);
    }

    @Test
    void showsErrorOnlyWhenGiven() {
        assertTrue(FormView.form("/briefs/new", FIELDS, Map.of(), "Date is invalid.").contains("Date is invalid."));
        assertFalse(FormView.form("/briefs/new", FIELDS, Map.of(), null).contains("role=\"alert\""));
    }

    @Test
    void usesNoStylesOrScripts() {
        String html = FormView.form("/briefs/new", FIELDS, Map.of(), "Bad");

        assertFalse(html.contains("style="));
        assertFalse(html.contains("<script"));
    }
}
