package org.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.model.RecordType;
import org.model.UserAccount;
import org.view.HtmlPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Serves /menu, /briefs, /agents, /facilities, /missions, /audit and /logout for logged-in users.
// Requests without a valid session cookie are redirected to the login page.
public class PageHandler implements HttpHandler {

    private final AppController controller;
    private final SessionManager sessions;

    public PageHandler(AppController controller, SessionManager sessions) {
        if (controller == null || sessions == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.controller = controller;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String token = SessionManager.tokenFromCookieHeader(exchange.getRequestHeaders().getFirst("Cookie"));
        UserAccount user = sessions.getUser(token);
        if (user == null) {
            WebServer.redirect(exchange, "/");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/logout")) {
            sessions.endSession(token);
            WebServer.redirect(exchange, "/");
            return;
        }
        if (path.equals("/menu")) {
            String body = "<p>Logged in as " + HtmlPage.escape(user.getUsername()) + ".</p>\n"
                    + "<ul>\n<li><a href=\"/briefs\">Briefs</a></li>\n<li><a href=\"/agents\">Agents</a></li>\n"
                    + "<li><a href=\"/facilities\">Facilities</a></li>\n<li><a href=\"/missions\">Missions</a></li>\n"
                    + (user.isAdmin() ? "<li><a href=\"/audit\">Review audit log</a></li>\n" : "") + "</ul>";
            send(exchange, user, 200, "Main Menu", body);
            return;
        }
        if (path.equals("/audit")) {
            send(exchange, user, 200, "Audit Log", HtmlPage.list(controller.auditLog(user), null));
            return;
        }

        // /briefs, /briefs/3, /agents, /agents/2, /facilities, /facilities/1, /missions, /missions/4
        String[] parts = path.substring(1).split("/");
        RecordType type = typeFor(parts[0]);
        if (type == null || parts.length > 2) {
            send(exchange, user, 404, "Not Found", "<p>Page not found.</p>");
        } else if (parts.length == 1) {
            String text = controller.listRecords(type);
            send(exchange, user, 200, type.getTitle(), HtmlPage.list(text, links(parts[0], text)));
        } else {
            try {
                int id = Integer.parseInt(parts[1]);
                send(exchange, user, 200, type.getTitle(), HtmlPage.paragraphs(controller.readRecord(type, id, user)));
            } catch (NumberFormatException e) {
                send(exchange, user, 404, "Not Found", "<p>Invalid ID.</p>");
            }
        }
    }

    private static RecordType typeFor(String segment) {
        return switch (segment) {
            case "briefs" -> RecordType.BRIEF;
            case "agents" -> RecordType.AGENT;
            case "facilities" -> RecordType.FACILITY;
            case "missions" -> RecordType.MISSION;
            default -> null;
        };
    }

    // Links each "id. name" line of a listing to its record page.
    private static List<String> links(String segment, String text) {
        List<String> hrefs = new ArrayList<>();
        for (String line : text.split("\n")) {
            int dot = line.indexOf(". ");
            hrefs.add(dot > 0 && line.substring(0, dot).matches("\\d+") ? "/" + segment + "/" + line.substring(0, dot) : null);
        }
        return hrefs;
    }

    private static void send(HttpExchange exchange, UserAccount user, int status, String title, String body)
            throws IOException {
        WebServer.send(exchange, status, HtmlPage.page(title, body, true, user.isAdmin()));
    }
}
