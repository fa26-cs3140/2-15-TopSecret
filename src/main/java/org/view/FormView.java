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
        StringBuilder html = new StringBuilder();
        if (error != null && !error.isBlank()) {
            html.append("<p role=\"alert\"><strong>").append(HtmlPage.escape(error)).append("</strong></p>\n");
        }
        html.append("<form method=\"post\" action=\"").append(HtmlPage.escape(action)).append("\">\n");
        for (FormField field : fields) {
            String value = values == null ? null : values.get(field.name());
            html.append("<p><label for=\"").append(field.name()).append("\">").append(HtmlPage.escape(field.label()))
                .append("</label>\n").append(input(field, value)).append("</p>\n");
        }
        return html.append("<p><button type=\"submit\">Save</button></p>\n</form>").toString();
    }

    // One input element for a field, filled with value when it is not null.
    private static String input(FormField field, String value) {
        String common = "id=\"" + field.name() + "\" name=\"" + field.name() + "\"" + (field.required() ? " required" : "");
        String safe = HtmlPage.escape(value);
        if (field.kind() == FormField.Kind.TEXTAREA) {
            return "<textarea " + common + " rows=\"5\">" + safe + "</textarea>";
        }
        String type = switch (field.kind()) {
            case DATE -> "date";
            case NUMBER -> "number\" min=\"1";
            default -> "text";
        };
        return "<input type=\"" + type + "\" " + common + " value=\"" + safe + "\">";
    }
}
