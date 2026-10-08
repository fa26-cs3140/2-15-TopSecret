package org.example;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class UserInterfaceTest {

    /*
     * Simple fake DatabaseManager used so UserInterface can be
     * unit tested without accessing SQLite.
     */
    static class FakeDatabaseManager extends DatabaseManager {

        private final String[] titles = {
                "Operation Sandtrap",
                "The Munich Lead",
                "Project Bluebird"
        };

        private final String[] records = {
                "Operation Sandtrap\t1970-11-03\tBug the diplomatic pouch.",
                "The Munich Lead\t1972-09-15\tIdentify the logistical backbone.",
                "Project Bluebird\t1973-04-20\tExfiltrate the defector."
        };

        @Override
        public String[] getColumn(String column) {
            if ("Title".equals(column)) {
                return titles;
            }

            return null;
        }

        @Override
        public String[] getRecords(String column, String value) {
            if (!"Title".equals(column)) {
                return null;
            }

            if ("%".equals(value)) {
                return records;
            }

            for (String record : records) {
                String[] fields = record.split("\t", 3);

                if (fields[0].equals(value)) {
                    return new String[]{record};
                }
            }

            return new String[0];
        }
    }

    private UserInterface createInterface(
            String input,
            ByteArrayOutputStream output
    ) {
        return new UserInterface(
                new FakeDatabaseManager(),
                new Scanner(input),
                new PrintStream(output)
        );
    }

    @Test
    void displayMenuShowsRequiredOptions() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        UserInterface ui = createInterface("", output);

        ui.displayMenu();

        String text = output.toString();

        assertTrue(text.contains("List available mission briefs"));
        assertTrue(text.contains("Read a mission brief"));
        assertTrue(text.contains("Exit"));
    }

    @Test
    void displayMissionListShowsNumberedTitles() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        UserInterface ui = createInterface("", output);

        ui.displayMissionList();

        String text = output.toString();

        assertTrue(text.contains("1. Operation Sandtrap"));
        assertTrue(text.contains("2. The Munich Lead"));
        assertTrue(text.contains("3. Project Bluebird"));
    }

    @Test
    void displayMissionShowsSelectedMission() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        UserInterface ui = createInterface("", output);

        ui.displayMission(2);

        String text = output.toString();

        assertTrue(text.contains("The Munich Lead"));
        assertTrue(text.contains("1972-09-15"));
        assertTrue(text.contains("Identify the logistical backbone."));
    }

    @Test
    void displayMissionRejectsNumberThatIsTooSmall() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        UserInterface ui = createInterface("", output);

        ui.displayMission(0);

        assertTrue(
                output.toString().contains("Invalid mission number")
        );
    }

    @Test
    void displayMissionRejectsNumberThatIsTooLarge() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        UserInterface ui = createInterface("", output);

        ui.displayMission(100);

        assertTrue(
                output.toString().contains("Invalid mission number")
        );
    }

    @Test
    void runInterfaceCanListThenExit() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        /*
         * 1 = list missions
         * 3 = exit
         */
        UserInterface ui = createInterface(
                "1\n3\n",
                output
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("1. Operation Sandtrap"));
        assertTrue(text.contains("2. The Munich Lead"));
        assertTrue(text.contains("Exiting"));
    }

    @Test
    void runInterfaceCanReadMissionThenReturnToMenu() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        /*
         * 2 = read mission
         * 1 = mission number
         * 3 = exit
         */
        UserInterface ui = createInterface(
                "2\n1\n3\n",
                output
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("Operation Sandtrap"));
        assertTrue(text.contains("1970-11-03"));

        /*
         * The menu should have been displayed once before reading
         * the mission and again afterward.
         */
        assertTrue(
                countOccurrences(text, "List available mission briefs") >= 2
        );
    }

    @Test
    void invalidMenuInputDoesNotEndProgram() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        UserInterface ui = createInterface(
                "hello\n3\n",
                output
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("Invalid menu selection"));
        assertTrue(text.contains("Exiting"));
    }

    @Test
    void invalidMissionInputDoesNotEndProgram() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        /*
         * 2 = read
         * hello = invalid mission number
         * 3 = exit
         */
        UserInterface ui = createInterface(
                "2\nhello\n3\n",
                output
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("Invalid mission number"));
        assertTrue(text.contains("Exiting"));
    }

    @Test
    void missingMissionDataIsHandledGracefully() {

        DatabaseManager brokenDatabase = new DatabaseManager() {
            @Override
            public String[] getColumn(String column) {
                return null;
            }

            @Override
            public String[] getRecords(String column, String value) {
                return null;
            }
        };

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        UserInterface ui = new UserInterface(
                brokenDatabase,
                new Scanner(""),
                new PrintStream(output)
        );

        ui.displayMissionList();

        assertTrue(
                output.toString().contains("Unable to load missions")
        );
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }

        return count;
    }
}