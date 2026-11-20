package org.model;

/** A related record that a detail view can display and link to by type and ID. */
public record RelatedRecord(RecordType type, int id, String label) {
}
