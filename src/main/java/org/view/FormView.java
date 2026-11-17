package org.view;

import java.util.List;
import java.util.Map;

// Plain HTML create/edit forms for the web view. No inline styles or JavaScript;
// Team A's stylesheet decorates the markup.
public final class FormView {

    private FormView() {
        // Utility class -> not instantiated
    }

    /**
     * Renders a form that POSTs to action, with one labelled input per field.
     *
     * @param action URL the form submits to, for example /briefs/new or /briefs/3/edit
     * @param fields inputs in display order
     * @param values current values by field name; an empty map gives an empty form
     * @param error  message shown above the form, or null for none
     */
    public static String form(String action, List<FormField> fields, Map<String, String> values, String error) {
        throw new UnsupportedOperationException("HW7 week 2: render the create/edit form");
    }
}
