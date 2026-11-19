package org.view;

import org.model.RelatedRecord;

import java.util.List;
import java.util.Locale;

/** Escapes related-record labels and builds links from application record types and IDs. */
public final class RelatedRecordView {

    private RelatedRecordView() {
    }

    public static String links(List<RelatedRecord> records) {
        if (records.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<h3>Related records</h3>\n<ul>\n");
        for (RelatedRecord record : records) {
            String segment = record.type().getTitle().toLowerCase(Locale.ROOT);
            html.append("<li><a href=\"/").append(segment).append("/").append(record.id()).append("\">")
                    .append(HtmlPage.escape(record.type().getSingular())).append(" · ")
                    .append(HtmlPage.escape(record.label())).append("</a></li>\n");
        }
        return html.append("</ul>\n").toString();
    }
}
