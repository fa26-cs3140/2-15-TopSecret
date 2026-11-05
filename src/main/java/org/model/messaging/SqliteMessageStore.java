package org.model.messaging;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Additive persistence; requires the shared users and agents tables. */
public final class SqliteMessageStore implements MessageStore {
    private final String url;
    public SqliteMessageStore(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("Database URL required.");
        this.url = url;
        try (var resource = getClass().getResourceAsStream("/org/model/messaging/schema.sql");
             var c = connect(); var s = c.createStatement()) {
            if (resource == null) throw new IllegalStateException("Missing message schema.");
            for (String sql : new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split(";"))
                if (!sql.isBlank()) s.execute(sql);
        } catch (java.io.IOException | SQLException e) { throw new IllegalStateException("Cannot initialize messages.", e); }
    }
    private Connection connect() throws SQLException {
        var c = DriverManager.getConnection(url);
        try (var s = c.createStatement()) { s.execute("PRAGMA foreign_keys=ON"); }
        catch (SQLException e) { c.close(); throw e; }
        return c;
    }
    @Override public Message save(Message m) {
        if (m == null || m.getId() != 0) throw new IllegalArgumentException("An unsaved message is required.");
        // Resolve the recipient snapshot inside SQL, rather than trusting client-provided names.
        String sql = "INSERT INTO agent_messages(sender_username,recipient_agent_id,recipient_name,body,sent_at) "
                + "SELECT ?,id,name,?,? FROM agents WHERE id=? AND name=?";
        try (var c = connect(); var p = c.prepareStatement(sql)) {
            p.setString(1,m.getSenderUsername()); p.setString(2,m.getBody());
            p.setString(3,m.getSentAt().toString()); p.setInt(4,m.getRecipientAgentId()); p.setString(5,m.getRecipientName());
            if (p.executeUpdate()!=1) throw new IllegalArgumentException("Agent no longer exists or was renamed. Please select again.");
            try(var s=c.createStatement();var rs=s.executeQuery("SELECT last_insert_rowid()")) {
                rs.next(); return new Message(rs.getLong(1),m.getSenderUsername(),m.getRecipientAgentId(),m.getRecipientName(),m.getBody(),m.getSentAt());
            }
        } catch(SQLException e) {throw new IllegalStateException("Unable to save message.",e);}
    }
    @Override public List<Message> findForRecipient(String name) {
        if(name==null || name.isBlank()) throw new IllegalArgumentException("Recipient required.");
        try(var c=connect();var p=c.prepareStatement("SELECT * FROM agent_messages WHERE recipient_name=? COLLATE NOCASE ORDER BY id")) {
            p.setString(1,name);var result=new ArrayList<Message>();
            try(var rs=p.executeQuery()) {while(rs.next()) result.add(new Message(rs.getLong("id"),rs.getString("sender_username"),
                    rs.getInt("recipient_agent_id"),rs.getString("recipient_name"),rs.getString("body"),LocalDateTime.parse(rs.getString("sent_at"))));}
            return List.copyOf(result);
        } catch(SQLException e) {throw new IllegalStateException("Unable to read messages.",e);}
    }
}
