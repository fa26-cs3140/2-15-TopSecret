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
            .append("<link rel=\"stylesheet\" href=\"/css/style.css\">\n")
            .append("</head>\n<body>\n")
            .append("<h1 class = \"topdown-on-load\">Top Secret</h1>\n");

        // Navbar (only available after login)
        if (loggedIn) {
            html.append("<nav aria-label=\"Main navigation\">\n")
                .append("<a class = \"topdown-on-load\" href=\"/menu\">Menu</a> | ")
                .append("<a class = \"topdown-on-load\" href=\"/briefs\">Briefs</a> | ")
                .append("<a class = \"topdown-on-load\" href=\"/agents\">Agents</a> | ")
                .append("<a class = \"topdown-on-load\" href=\"/facilities\">Facilities</a> | ")
                .append("<a class = \"topdown-on-load\" href=\"/countries\">Countries</a> |")
                .append("<a class = \"topdown-on-load\" href=\"/missions\">Missions</a> | ")
                .append("<a class = \"topdown-on-load\" href=\"/messages\">Messages</a>");

            // TODO: add Missions when the controller supports it

            // Can review audit log if logged in as admin
            if (admin) {
                html.append(" | <a class = \"topdown-on-load\" href=\"/audit\">Audit log</a>");
            }

            html.append(" | <a class = \"topdown-on-load\" href=\"/logout\">Log out</a>\n")
                .append("</nav>\n<hr>\n");
        }

        html.append("<main>\n<h2 class = \"topdown-on-load\">")
            .append(safeTitle)
            .append("</h2>\n")
            .append(body == null ? "" : body)
            .append("\n</main>\n</body>\n</html>\n");

        return html.toString();
    }

    /**
     * Converts terminal text (which is very line-oriented) into HTML list.
     * Each item can link to an internal application URL.
     */
    public static String list(String text, List<String> hrefs) {
        String[] lines = (text == null ? "" : text).split("\\R", -1); // '\\R' handles any line break e.g. \n, \r\n
        StringBuilder html = new StringBuilder("<ul>\n");

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;

            String href; // getting the matching URL for each item
            if (hrefs == null || i >= hrefs.size())
                href = null;
            else
                href = hrefs.get(i);

            String item = escape(lines[i]);

            html.append("<li>");

            if (isSafeInternalPath(href)) {
                html.append("<a href=\"")
                    .append(escape(href))
                    .append("\">")
                    .append(item)
                    .append("</a>");
            }
            else {
                html.append(item);
            }

            html.append("</li>\n");
        }

        return html.append("</ul>").toString();
    }

    /**
     * Helper method: Only allows simple internal application paths
     */
    private static boolean isSafeInternalPath(String href) {
        return href != null && href.matches("/(?!/)[A-Za-z0-9_/-]*");
    }

    /**
     * Converts record details into HTML paragraphs
     */
    public static String paragraphs(String text) {
        if (text == null || text.isEmpty()) return "";

        StringBuilder html = new StringBuilder();

        for (String line : text.split("\\R")) {
            html.append("<p>")
                .append(escape(line))
                .append("</p>\n");
        }

        return html.toString();
    }

    /**
     * Creates the login form and displays error if necessary
     */
    public static String loginForm(String error) {
        StringBuilder html = new StringBuilder();

        if (error != null && !error.isBlank()) {
            html.append("<p role=\"alert\"><strong>")
                .append(escape(error))
                .append("</strong></p>\n");
        }

        html.append("<form method=\"post\" action=\"/login\">\n")
            .append("<p><label for=\"username\">Username:</label> ")
            .append("<input id=\"username\" name=\"username\" ")
            .append("type=\"text\" autocomplete=\"username\" required>")
            .append("</p>\n")
            .append("<p><label for=\"password\">Password:</label> ")
            .append("<input id=\"password\" name=\"password\" ")
            .append("type=\"password\" ")
            .append("autocomplete=\"current-password\" required>")
            .append("</p>\n")
            .append("<p><button type=\"submit\">Log in</button></p>\n")
            .append("</form>");

        return html.toString();
    }

    /**
     * Escapes characters that could be interpreted as HTML.
     */
    public static String escape(String text) {
        // just making it look a little cleaner
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
