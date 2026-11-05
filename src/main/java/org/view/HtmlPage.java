package org.view;

import java.util.List;

// Plain HTML template for the web view (no CSS or JavaScript).
// The body is built from the same text the terminal prints, with a few tags added.
public final class HtmlPage {

    private HtmlPage() {
    }

    // Full page with a navigation list; loggedIn hides the links on the login page.
    public static String page(String title, String body, boolean loggedIn, boolean admin) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head><meta charset=\"utf-8\"><title>Top Secret - ")
                .append(escape(title)).append("</title></head>\n<body>\n<h1>Top Secret</h1>\n");
        if (loggedIn) {
            html.append("<p><a href=\"/menu\">Menu</a> | <a href=\"/briefs\">Briefs</a> | ")
                    .append("<a href=\"/agents\">Agents</a> | <a href=\"/facilities\">Facilities</a>");
            if (admin) {
                html.append(" | <a href=\"/audit\">Audit log</a>");
            }
            html.append(" | <a href=\"/logout\">Log out</a></p>\n");
        }
        html.append("<h2>").append(escape(title)).append("</h2>\n").append(body).append("\n</body>\n</html>\n");
        return html.toString();
    }

    // One list item per line of text; each item links to href(index) when it is not null.
    public static String list(String text, List<String> hrefs) {
        String[] lines = text.split("\n");
        StringBuilder html = new StringBuilder("<ul>\n");
        for (int i = 0; i < lines.length; i++) {
            String href = hrefs == null || i >= hrefs.size() ? null : hrefs.get(i);
            String item = escape(lines[i]);
            html.append("<li>").append(href == null ? item : "<a href=\"" + href + "\">" + item + "</a>")
                    .append("</li>\n");
        }
        return html.append("</ul>").toString();
    }

    // Each line becomes a paragraph.
    public static String paragraphs(String text) {
        StringBuilder html = new StringBuilder();
        for (String line : text.split("\n")) {
            html.append("<p>").append(escape(line)).append("</p>\n");
        }
        return html.toString();
    }

    public static String loginForm(String error) {
        String message = error == null ? "" : "<p><strong>" + escape(error) + "</strong></p>\n";
        return message + "<form method=\"post\" action=\"/login\">\n"
                + "<p><label>Username: <input type=\"text\" name=\"username\"></label></p>\n"
                + "<p><label>Password: <input type=\"password\" name=\"password\"></label></p>\n"
                + "<p><input type=\"submit\" value=\"Log in\"></p>\n</form>";
    }

    public static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
