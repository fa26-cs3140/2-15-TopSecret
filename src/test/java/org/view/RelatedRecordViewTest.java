package org.view;

import org.junit.jupiter.api.Test;
import org.model.RecordType;
import org.model.RelatedRecord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatedRecordViewTest {

    @Test
    void linksUseTypedRoutesAndEscapeLabels() {
        String html = RelatedRecordView.links(List.of(
                new RelatedRecord(RecordType.FACILITY, 7, "<script>& HQ"),
                new RelatedRecord(RecordType.MISSION, 8, "Recon")));

        assertTrue(html.contains("href=\"/facilities/7\""));
        assertTrue(html.contains("href=\"/missions/8\""));
        assertTrue(html.contains("&lt;script&gt;&amp; HQ"));
        assertFalse(html.contains("<script>"));
        assertEquals("", RelatedRecordView.links(List.of()));
    }
}
