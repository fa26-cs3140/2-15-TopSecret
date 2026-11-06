package org.view;

import java.util.List;

// Bare HTML template so the web view runs. Team C: build the real view here.
public final class HtmlPage {

    private HtmlPage() {
    }

    public static String page(String title, String body, boolean loggedIn, boolean admin) {
        return "<html><body>\n" + body + "\n</body></html>";
    }

    public static String list(String text, List<String> hrefs) {
        String[] lines = text.split("\n");
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String href = hrefs == null || i >= hrefs.size() ? null : hrefs.get(i);
            html.append(href == null ? escape(lines[i]) : "<a href=\"" + href + "\">" + escape(lines[i]) + "</a>")
                    .append("<br>\n");
        }
        return html.toString();
    }

    public static String paragraphs(String text) {
        return "<pre>" + escape(text) + "</pre>";
    }

    public static String loginForm(String error) {
        return (error == null ? "" : escape(error) + "<br>\n")
                + "<form method=\"post\" action=\"/login\">\n"
                + "<input name=\"username\"><input type=\"password\" name=\"password\"><input type=\"submit\">\n"
                + "</form>";
    }

    public static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}