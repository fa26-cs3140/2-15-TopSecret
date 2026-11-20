package org.model.messaging;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite implementation of MessageStore.
 * Stores messages in the shared database containing users and agents.
 */
public final class SqliteMessageStore implements MessageStore {

    private static final String SCHEMA_PATH = "/org/model/messaging/schema.sql";

    private static final String INSERT_MESSAGE = """
            INSERT INTO agent_messages (
                sender_username,
                recipient_agent_id,
                recipient_name,
                body,
                sent_at
            )
            SELECT ?, id, name, ?, ?
            FROM agents
            WHERE id = ? AND name = ?
            """;

    private static final String SELECT_INBOX = """
            SELECT *
            FROM agent_messages
            WHERE recipient_name = ? COLLATE NOCASE
            ORDER BY id
            """;


    private static final String MARK_AS_READ = """
            UPDATE agent_messages
            SET is_read = 1
            WHERE id = ? AND recipient_name = ? COLLATE NOCASE
            """;

    private static final String SELECT_MESSAGE = """
            SELECT *
            FROM agent_messages
            WHERE id = ? AND recipient_name = ? COLLATE NOCASE
            """;

    private final String url;

    public SqliteMessageStore(String url) {
        if (url == null || url.isBlank())
            throw new IllegalArgumentException("Database URL required.");

        this.url = url;
        initializeSchema();
    }

    /**
     * Helper: Initializes the messaging table using the SQL schema resource.
     */
    private void initializeSchema() {
        try (var resource = getClass().getResourceAsStream(SCHEMA_PATH);
             Connection connection = connect();
             Statement statement = connection.createStatement()) {

            if (resource == null)
                throw new IllegalStateException("Missing message schema.");

            String schema = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

            for (String sql : schema.split(";")) {
                if (!sql.isBlank())
                    statement.execute(sql);
            }

            // Check whether is_read column exists
            // If not, SQLite adds and marks existing messages as unread by default
            migrateReadStatus(connection);

        } catch (java.io.IOException | SQLException e) {
            throw new IllegalStateException("Cannot initialize messages.", e);
        }
    }

    /** Helper: Opens a database connection with foreign key enforcement enabled. */
    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }

        return connection;
    }


    /** Helper: Adds the read-status column to databases (created before HW 7) */
    private void migrateReadStatus(Connection connection) throws SQLException {
        boolean columnExists = false;

        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(agent_messages)")) {

            while (columns.next()) {
                if ("is_read".equalsIgnoreCase(columns.getString("name"))) {
                    columnExists = true;
                    break;
                }
            }
        }

        if (!columnExists) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    ALTER TABLE agent_messages
                    ADD COLUMN is_read INTEGER NOT NULL DEFAULT 0
                    CHECK(is_read IN (0, 1))
                    """);
            }
        }
    }

    @Override
    public Message save (Message message) {
        if (message == null || message.getId() != 0)
            throw new IllegalArgumentException("An unsaved message is required.");

        // Resolve the recipient snapshot inside SQL, rather than trusting client-provided names.
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(INSERT_MESSAGE)) {

            statement.setString(1, message.getSenderUsername());
            statement.setString(2, message.getBody());
            statement.setString(3, message.getSentAt().toString());
            statement.setInt(4, message.getRecipientAgentId());
            statement.setString(5, message.getRecipientName());

            int rowsInserted = statement.executeUpdate();

            if (rowsInserted != 1) {
                throw new IllegalArgumentException(
                        "Agent no longer exists or was renamed. Please select again."
                );
            }

            long messageId = getLastInsertedId(connection);

            return new Message(
                    messageId,
                    message.getSenderUsername(),
                    message.getRecipientAgentId(),
                    message.getRecipientName(),
                    message.getBody(),
                    message.getSentAt()
            );

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to save message.", e);
        }
    }

    /** Helper: Retrieves the ID of the most recently inserted record */
    private long getLastInsertedId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {

            result.next();
            return result.getLong(1);
        }
    }

    @Override
    public List<Message> findForRecipient(String name) {
        if (name==null || name.isBlank())
            throw new IllegalArgumentException("Recipient required.");

        List<Message> messages = new ArrayList<>();

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(SELECT_INBOX)) {

            statement.setString(1, name);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    messages.add(readMessage(result));
                }
            }

            return List.copyOf(messages);

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read messages.", e);
        }
    }

    /** Helper: Converts a database result row into a Message object */
    private Message readMessage(ResultSet result) throws SQLException {
        return new Message(
                result.getLong("id"),
                result.getString("sender_username"),
                result.getInt("recipient_agent_id"),
                result.getString("recipient_name"),
                result.getString("body"),
                LocalDateTime.parse(result.getString("sent_at"))
        );
    }
}
