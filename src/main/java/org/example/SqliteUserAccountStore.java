package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite implementation of UserAccountStore.
 * Stores usernames and access-level metadata only. Passwords remain in ciphered files.
 */
public class SqliteUserAccountStore implements UserAccountStore {

    private final String url;

    public SqliteUserAccountStore(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Database URL cannot be blank.");
        }
        this.url = url;
        createTable();
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean hasUsers() {
        String sql = "SELECT 1 FROM users LIMIT 1";
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to access user metadata.", e);
        }
    }

    @Override
    public boolean addUser(UserAccount account) {
        if (account == null) {
            throw new IllegalArgumentException("User account cannot be null.");
        }

        String sql = "INSERT INTO users (username, is_admin) VALUES (?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getUsername());
            statement.setInt(2, account.isAdmin() ? 1 : 0);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public UserAccount getUser(String username) {
        String sql = "SELECT username, is_admin FROM users WHERE username = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new UserAccount(
                        result.getString("username"),
                        result.getInt("is_admin") != 0
                );
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read user metadata.", e);
        }
    }

    @Override
    public List<UserAccount> getUsers() {
        List<UserAccount> users = new ArrayList<>();
        String sql = "SELECT username, is_admin FROM users ORDER BY username";
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                users.add(new UserAccount(
                        result.getString("username"),
                        result.getInt("is_admin") != 0
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to read persisted records.", e);
        }
        return users;
    }

    @Override
    public boolean setAdmin(String username, boolean admin) {
        String sql = "UPDATE users SET is_admin = ? WHERE username = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, admin ? 1 : 0);
            statement.setString(2, username);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to access user metadata.", e);
        }
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "username TEXT NOT NULL PRIMARY KEY, "
                + "is_admin INTEGER NOT NULL DEFAULT 0)";
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize users table.", e);
        }
    }
}
