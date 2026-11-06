package org.view;

import java.util.List;

/**
 * HTML presentation for **read-only** web interface.
 *
 * AppController will supply the plain text record data used by
 * the terminal. This class will use that to ONLY format HTML,
 * not authentication or database access or HTTP responses.
 */
public final class HtmlPage {

    private HtmlPage() {
        // Utility class -> not instantiated
    }

    public static String page(String title, String body, boolean loggedIn, boolean admin) {
        String safeTitle = escape(title);
        StringBuilder html = new StringBuilder(); // StringBuilder used for convenience

        // header always include Top Secret heading and navbar (after logging in)
        html.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\">\n")
                .append("<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<title>Top Secret - ")
                .append(safeTitle)
                .append("</title>\n")
                .append("</head>\n<body>\n")
                .append("<h1>Top Secret</h1>\n");

        // Navbar (only available after login)
        if (loggedIn) {
            html.append("<nav aria-label=\"Main navigation\">\n")
                    .append("<a href=\"/menu\">Menu</a> | ")
                    .append("<a href=\"/briefs\">Briefs</a> | ")
                    .append("<a href=\"/agents\">Agents</a> | ")
                    .append("<a href=\"/facilities\">Facilities</a>");

            // TODO: add Missions when the controller supports it

            // Can review audit log if logged in as admin
            if (admin) {
                html.append(" | <a href=\"/audit\">Audit log</a>");
            }

            html.append(" | <a href=\"/logout\">Log out</a>\n")
                    .append("</nav>\n<hr>\n");
        }

        html.append("<main>\n<h2>")
                .append(safeTitle)
                .append("</h2>\n")
                .append(body == null ? "" : body)
                .append("\n</main>\n</body>\n</html>\n");

        return html.toString();
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