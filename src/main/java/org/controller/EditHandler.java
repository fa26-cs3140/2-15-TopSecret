package org.controller;

import com.sun.net.httpserver.HttpExchange;
import org.model.RecordType;
import org.model.UserAccount;
import org.view.FormView;
import org.view.HtmlPage;

import java.io.IOException;
import java.util.Map;

// Create and edit workflow for the web view (administrators only):
//   GET  /<type>/new         empty form          POST /<type>/new         create, redirect to /<type>/<id>
//   GET  /<type>/<id>/edit   pre-filled form     POST /<type>/<id>/edit   update, redirect to /<type>/<id>
// PageHandler checks the session first and passes the logged-in user here.
// Non-admins get 403; invalid input re-shows the form with the error and the typed values.
public class EditHandler {

    private final AppController controller;

    public EditHandler(AppController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("Controller cannot be null.");
        }
        this.controller = controller;
    }

    // An edit URL broken into its parts. id is null for /<type>/new.
    record EditPath(RecordType type, String segment, Integer id) {
    }

    // Parses /<type>/new and /<type>/<id>/edit; returns null for any other path.
    static EditPath parse(String path) {
        if (path == null || !path.startsWith("/")) {
            return null;
        }
        String[] parts = path.substring(1).split("/");
        RecordType type = parts.length > 1 ? PageHandler.typeFor(parts[0]) : null;
        if (type == null) {
            return null;
        }
        if (parts.length == 2 && parts[1].equals("new")) {
            return new EditPath(type, parts[0], null);
        }
        if (parts.length == 3 && parts[2].equals("edit") && parts[1].matches("\\d{1,9}")) {
            return new EditPath(type, parts[0], Integer.parseInt(parts[1]));
        }
        return null;
    }

    // Runs the GET (show form) or POST (save) step for an edit URL.
    public void handle(HttpExchange exchange, UserAccount user) throws IOException {
        EditPath path = parse(exchange.getRequestURI().getPath());
        if (path == null) {
            send(exchange, user, 404, "Not Found", "<p>Page not found.</p>");
            return;
        }
        if (!controller.canEdit(user)) {
            send(exchange, user, 403, "Access Denied",
                    "<p>Access denied. Only administrators can create or edit records.</p>");
            return;
        }
        Map<String, String> current = path.id() == null ? Map.of() : controller.formValues(path.type(), path.id());
        if (current == null) {
            send(exchange, user, 404, "Not Found",
                    "<p>No " + path.type().getSingular() + " found with ID " + path.id() + ".</p>");
            return;
        }
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            save(exchange, user, path);
        } else {
            showForm(exchange, user, path, current, null);
        }
    }

    // Creates or updates the record, then redirects to it; invalid input re-shows the form.
    private void save(HttpExchange exchange, UserAccount user, EditPath path) throws IOException {
        Map<String, String> submitted = WebServer.readForm(exchange);
        try {
            int id;
            if (path.id() == null) {
                id = controller.createRecord(path.type(), submitted, user);
            } else {
                controller.updateRecord(path.type(), path.id(), submitted, user);
                id = path.id();
            }
            WebServer.redirect(exchange, "/" + path.segment() + "/" + id);
        } catch (IllegalArgumentException e) {
            showForm(exchange, user, path, submitted, e.getMessage());
        }
    }

    private void showForm(HttpExchange exchange, UserAccount user, EditPath path, Map<String, String> values,
                          String error) throws IOException {
        String singular = path.type().getSingular();
        String title = (path.id() == null ? "New " : "Edit ") + singular;
        String action = "/" + path.segment() + (path.id() == null ? "/new" : "/" + path.id() + "/edit");
        String cancel = "/" + path.segment() + (path.id() == null ? "" : "/" + path.id());
        String body = FormView.form(action, RecordForms.fieldsFor(path.type()), values, error)
                + "\n<p><a href=\"" + cancel + "\">Cancel</a></p>";
        send(exchange, user, 200, title, body);
    }

    private static void send(HttpExchange exchange, UserAccount user, int status, String title, String body)
            throws IOException {
        WebServer.send(exchange, status, HtmlPage.page(title, body, true, user.isAdmin()));
    }
}
