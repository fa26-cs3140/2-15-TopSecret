package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class UserInterfaceIntegrationTest {

    private Path testDatabasePath;
    private DatabaseManager databaseManager;
    private MissionSearch missionSearch;

    @BeforeEach
    void setUp() throws Exception {

        testDatabasePath = Files.createTempFile(
                "topsecret-ui-test",
                ".db"
        );

        String url =
                "jdbc:sqlite:" +
                        testDatabasePath.toAbsolutePath();

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

            String insert =
                    "INSERT INTO mission_briefs " +
                            "(Title, Date, Text) VALUES (?, ?, ?)";

            try (
                    PreparedStatement preparedStatement =
                            connection.prepareStatement(insert)
            ) {
                preparedStatement.setString(
                        1,
                        "Operation Test"
                );

                preparedStatement.setDate(
                        2,
                        Date.valueOf("2001-01-01")
                );

                preparedStatement.setString(
                        3,
                        "Recover the diplomatic pouch."
                );

                preparedStatement.executeUpdate();

                preparedStatement.setString(
                        1,
                        "Second Mission"
                );

                preparedStatement.setDate(
                        2,
                        Date.valueOf("2002-02-02")
                );

                preparedStatement.setString(
                        3,
                        "Monitor the northern checkpoint."
                );

                preparedStatement.executeUpdate();
            }
        }

        databaseManager =
                new DatabaseManager(url);

        missionSearch =
                new MissionSearch(databaseManager);
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
                missionSearch,
                new Scanner(""),
                new PrintStream(output)
        );

        ui.displayMissionList();

        String text = output.toString();

        assertTrue(
                text.contains("1. Operation Test")
        );

        assertTrue(
                text.contains("2. Second Mission")
        );
    }

    @Test
    void userInterfaceDisplaysSelectedDatabaseMission() {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        UserInterface ui = new UserInterface(
                databaseManager,
                missionSearch,
                new Scanner(""),
                new PrintStream(output)
        );

        ui.displayMission(1);

        String text = output.toString();

        assertTrue(text.contains("Operation Test"));
        assertTrue(text.contains("2001-01-01"));

        assertTrue(
                text.contains(
                        "Recover the diplomatic pouch."
                )
        );
    }

    @Test
    void userInterfaceDisplaysRealSearchResults() {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        UserInterface ui = new UserInterface(
                databaseManager,
                missionSearch,
                new Scanner(
                        "3\ndiplomatic pouch\n4\n"
                ),
                new PrintStream(output)
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("Search Results"));
        assertTrue(text.contains("Operation Test"));
        assertTrue(text.contains("2001-01-01"));

        assertTrue(
                text.contains(
                        "Recover the diplomatic pouch."
                )
        );
    }

    @Test
    void userInterfaceDisplaysRealNoMatchesMessage() {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        UserInterface ui = new UserInterface(
                databaseManager,
                missionSearch,
                new Scanner(
                        "3\nnot-in-any-mission\n4\n"
                ),
                new PrintStream(output)
        );

        ui.runInterface();

        assertTrue(
                output.toString().contains(
                        "No matches were found."
                )
        );
    }

    @Test
    void multipleActionsWorkInOneSession() {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        /*
         * 1 = list missions
         * 2 = read a mission
         * 2 = mission number
         * 3 = search
         * northern = search phrase
         * 4 = exit
         */
        UserInterface ui = new UserInterface(
                databaseManager,
                missionSearch,
                new Scanner(
                        "1\n2\n2\n3\nnorthern\n4\n"
                ),
                new PrintStream(output)
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(
                text.contains("1. Operation Test")
        );

        assertTrue(
                text.contains("2. Second Mission")
        );

        assertTrue(
                text.contains(
                        "Monitor the northern checkpoint."
                )
        );

        assertTrue(text.contains("Search Results"));
        assertTrue(text.contains("Exiting"));
    }
}