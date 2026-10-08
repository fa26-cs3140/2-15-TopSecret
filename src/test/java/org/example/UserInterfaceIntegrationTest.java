package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class UserInterfaceIntegrationTest {

    private Path testDatabasePath;
    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() throws Exception {

        testDatabasePath = Files.createTempFile(
                "topsecret-ui-test",
                ".db"
        );

        String url =
                "jdbc:sqlite:" + testDatabasePath.toAbsolutePath();

        /*
         * Direct SQL is used only to create the integration-test
         * fixture. UserInterface itself never accesses SQLite.
         */
        try (
                Connection connection =
                        DriverManager.getConnection(url);

                Statement statement =
                        connection.createStatement()
        ) {
            statement.execute(
                    "CREATE TABLE mission_briefs (" +
                            "Title TEXT, " +
                            "Date DATE, " +
                            "Text TEXT)"
            );

            statement.executeUpdate(
                    "INSERT INTO mission_briefs " +
                            "(Title, Date, Text) VALUES " +
                            "('Operation Test', " +
                            "'2001-01-01', " +
                            "'Integration test mission.')"
            );

            statement.executeUpdate(
                    "INSERT INTO mission_briefs " +
                            "(Title, Date, Text) VALUES " +
                            "('Second Mission', " +
                            "'2002-02-02', " +
                            "'Second integration mission.')"
            );
        }

        databaseManager = new DatabaseManager(url);
    }

    @AfterEach
    void cleanUp() throws Exception {
        Files.deleteIfExists(testDatabasePath);
    }

    @Test
    void userInterfaceDisplaysDatabaseMissionList() {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        UserInterface ui = new UserInterface(
                databaseManager,
                new Scanner(""),
                new PrintStream(output)
        );

        ui.displayMissionList();

        String text = output.toString();

        assertTrue(text.contains("1. Operation Test"));
        assertTrue(text.contains("2. Second Mission"));
    }

    @Test
    void userInterfaceDisplaysSelectedDatabaseMission() {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        UserInterface ui = new UserInterface(
                databaseManager,
                new Scanner(""),
                new PrintStream(output)
        );

        ui.displayMission(1);

        String text = output.toString();

        assertTrue(text.contains("Operation Test"));
        assertTrue(text.contains("2001-01-01"));
        assertTrue(text.contains("Integration test mission."));
    }

    @Test
    void multipleDatabaseActionsWorkInOneSession() {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        /*
         * List missions
         * Read mission 2
         * Exit
         */
        UserInterface ui = new UserInterface(
                databaseManager,
                new Scanner("1\n2\n2\n3\n"),
                new PrintStream(output)
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("1. Operation Test"));
        assertTrue(text.contains("2. Second Mission"));
        assertTrue(text.contains("Second integration mission."));
        assertTrue(text.contains("Exiting"));
    }
}