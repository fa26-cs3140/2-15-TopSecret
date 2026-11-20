package org.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.model.RecordType;
import org.model.UserAccount;
import org.view.HtmlPage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Serves /menu, /audit, /logout and the record pages for logged-in users; create/edit URLs go to EditHandler.
// Requests without a valid session cookie are redirected to the login page.
public class PageHandler implements HttpHandler {

    private final AppController controller;
    private final SessionManager sessions;
    private final EditHandler edits;

    public PageHandler(AppController controller, SessionManager sessions) {
        if (controller == null || sessions == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.controller = controller;
        this.sessions = sessions;
        this.edits = new EditHandler(controller);
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
        if (EditHandler.parse(path) != null) {
            edits.handle(exchange, user);
            return;
        }
        switch (path) {
            case "/logout" -> {
                sessions.endSession(token);
                WebServer.redirect(exchange, "/");
            }
            case "/menu" -> showMenu(exchange, user);
            case "/audit" -> send(exchange, user, 200, "Audit Log", HtmlPage.list(controller.auditLog(user), null));
            default -> showRecords(exchange, user, path);
        }
    }

    private void showMenu(HttpExchange exchange, UserAccount user) throws IOException {
        String body = "<p>Logged in as " + HtmlPage.escape(user.getUsername()) + ".</p>\n"
                + "<ul>\n<li><a href=\"/briefs\">Briefs</a></li>\n<li><a href=\"/agents\">Agents</a></li>\n"
                + "<li><a href=\"/facilities\">Facilities</a></li>\n<li><a href=\"/missions\">Missions</a></li>\n"
                + "<li><a href=\"/countries\">Countries</a></li>\n"
                + (user.isAdmin() ? "<li><a href=\"/audit\">Review audit log</a></li>\n" : "") + "</ul>";
        send(exchange, user, 200, "Main Menu", body);
    }

    // /briefs, /briefs/3, /agents, /agents/2, /facilities, /facilities/1, /missions, /missions/4
    private void showRecords(HttpExchange exchange, UserAccount user, String path) throws IOException {
        String[] parts = path.substring(1).split("/");
        if (parts.length == 0) {
            send(exchange, user, 404, "Not Found", "<p>Page not found.</p>");
            return;
        }
        RecordType type = typeFor(parts[0]);
        if (type == null || parts.length > 2) {
            send(exchange, user, 404, "Not Found", "<p>Page not found.</p>");
        } else if (parts.length == 1) {
            showList(exchange, user, type, parts[0]);
        } else if (parts[1].matches("\\d{1,9}")) {
            showDetail(exchange, user, type, parts[0], Integer.parseInt(parts[1]));
        } else {
            send(exchange, user, 404, "Not Found", "<p>Invalid ID.</p>");
        }
    }

    // Admins also get a link to create a new record of this type.
    private void showList(HttpExchange exchange, UserAccount user, RecordType type, String segment)
            throws IOException {
        String text = controller.listRecords(type);
        String newLink = controller.canEdit(user)
                ? "<p><a href=\"/" + segment + "/new\">New " + type.getSingular() + "</a></p>\n" : "";
        send(exchange, user, 200, type.getTitle(), newLink + HtmlPage.list(text, links(segment, text)));
    }

    // Admins also get a link to edit the record when it exists.
    private void showDetail(HttpExchange exchange, UserAccount user, RecordType type, String segment, int id)
            throws IOException {
        if (controller.formValues(type, id) == null) {
            send(exchange, user, 404, "Not Found", "<p>Record not found.</p>");
            return;
        }
        String body = HtmlPage.paragraphs(controller.readRecord(type, id, user));
        body += org.view.RelatedRecordView.links(controller.relatedRecords(type, id));
        if (controller.canEdit(user)) {
            body += "<p><a href=\"/" + segment + "/" + id + "/edit\">Edit " + type.getSingular() + "</a></p>\n";
        }
        send(exchange, user, 200, type.getTitle(), body);
    }

    // Record type for a URL segment such as "briefs"; null when the segment is not a record type.
    static RecordType typeFor(String segment) {
        return switch (segment) {
            case "briefs" -> RecordType.BRIEF;
            case "agents" -> RecordType.AGENT;
            case "facilities" -> RecordType.FACILITY;
            case "countries" -> RecordType.COUNTRY;
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
