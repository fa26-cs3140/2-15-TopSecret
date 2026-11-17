package org.controller;

import com.sun.net.httpserver.HttpExchange;
import org.model.RecordType;
import org.model.UserAccount;

import java.io.IOException;

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
        throw new UnsupportedOperationException("HW7 week 2: create/edit workflow");
    }
}
