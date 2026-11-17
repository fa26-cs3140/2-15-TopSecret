package org.view;

// One input on a create/edit form. The controller decides which fields a record type has;
// FormView turns them into HTML.
public record FormField(String name, String label, Kind kind, boolean required) {

    // How the field is entered. ID_LIST is a comma-separated list of record IDs.
    public enum Kind { TEXT, TEXTAREA, DATE, NUMBER, ID_LIST }

    public FormField {
        if (name == null || name.isBlank() || label == null || label.isBlank() || kind == null) {
            throw new IllegalArgumentException("A form field needs a name, a label and a kind.");
        }
    }
}
