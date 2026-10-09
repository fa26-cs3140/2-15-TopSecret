package org.example;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Handles the interactive Homework 4 terminal interface.
 *
 * Mission data is obtained through DatabaseManager.
 * Search operations are performed through MissionSearch.
 * UserInterface does not directly access SQLite.
 */
public class UserInterface {

    private final DatabaseManager databaseManager;
    private final MissionSearch missionSearch;
    private final Scanner scanner;
    private final PrintStream output;

    /**
     * Creates the normal application UserInterface.
     */
    public UserInterface() {

        this.databaseManager =
                new DatabaseManager();

        this.missionSearch =
                new MissionSearch(databaseManager);

        this.scanner =
                new Scanner(System.in);

        this.output =
                System.out;
    }

    /**
     * Creates a UserInterface using the supplied database,
     * input, and output.
     *
     * A MissionSearch is created using the same DatabaseManager.
     *
     * @param databaseManager database access component
     * @param scanner input source
     * @param output output destination
     */
    public UserInterface(
            DatabaseManager databaseManager,
            Scanner scanner,
            PrintStream output
    ) {
        this(
                databaseManager,
                createMissionSearch(databaseManager),
                scanner,
                output
        );
    }

    /**
     * Creates a UserInterface using supplied dependencies.
     *
     * This constructor is useful for unit and integration testing.
     *
     * @param databaseManager database access component
     * @param missionSearch mission search component
     * @param scanner input source
     * @param output output destination
     */
    public UserInterface(
            DatabaseManager databaseManager,
            MissionSearch missionSearch,
            Scanner scanner,
            PrintStream output
    ) {

        if (databaseManager == null) {
            throw new IllegalArgumentException(
                    "DatabaseManager cannot be null."
            );
        }

        if (missionSearch == null) {
            throw new IllegalArgumentException(
                    "MissionSearch cannot be null."
            );
        }

        if (scanner == null) {
            throw new IllegalArgumentException(
                    "Scanner cannot be null."
            );
        }

        if (output == null) {
            throw new IllegalArgumentException(
                    "Output cannot be null."
            );
        }

        this.databaseManager =
                databaseManager;

        this.missionSearch =
                missionSearch;

        this.scanner =
                scanner;

        this.output =
                output;
    }

    /**
     * Runs the interactive mission menu until Exit is selected.
     */
    public void runInterface() {

        boolean running = true;

        while (running) {

            displayMenu();

            if (!scanner.hasNextLine()) {
                return;
            }

            String selection =
                    scanner.nextLine().trim();

            switch (selection) {

                case "1":
                    displayMissionList();
                    break;

                case "2":
                    readMissionSelection();
                    break;

                case "3":
                    runSearch();
                    break;

                case "4":
                    output.println(
                            "Exiting Top Secret."
                    );

                    running = false;
                    break;

                default:
                    output.println(
                            "Invalid menu selection. " +
                                    "Please try again."
                    );
                    break;
            }
        }
    }

    /**
     * Compatibility method for the existing TopSecret class.
     *
     * Homework 4 uses an interactive interface, so command-line
     * mission-selection arguments are no longer used here.
     *
     * @param args command-line arguments
     */
    public void runInterface(String[] args) {
        runInterface();
    }

    /**
     * Displays the main application menu.
     */
    public void displayMenu() {

        output.println();
        output.println(
                "Top Secret Mission Menu"
        );

        output.println(
                "-----------------------"
        );

        output.println(
                "1. List available mission briefs"
        );

        output.println(
                "2. Read a mission brief"
        );

        output.println(
                "3. Search mission briefs"
        );

        output.println(
                "4. Exit"
        );

        output.print(
                "Select an option: "
        );
    }

    /**
     * Displays all mission titles with corresponding numbers.
     */
    public void displayMissionList() {

        String[] titles =
                databaseManager.getColumn("Title");

        if (titles == null) {
            output.println(
                    "Unable to load missions."
            );
            return;
        }

        if (titles.length == 0) {
            output.println(
                    "No mission briefs are available."
            );
            return;
        }

        output.println();
        output.println(
                "Available Mission Briefs"
        );

        output.println(
                "------------------------"
        );

        for (
                int i = 0;
                i < titles.length;
                i++
        ) {
            output.println(
                    (i + 1) +
                            ". " +
                            titles[i]
            );
        }
    }

    /**
     * Displays a mission selected using its displayed number.
     *
     * @param missionNumber one-based mission number
     */
    public void displayMission(
            int missionNumber
    ) {

        if (missionNumber < 1) {
            output.println(
                    "Invalid mission number."
            );
            return;
        }

        String[] records =
                databaseManager.getRecords(
                        "Title",
                        "%"
                );

        if (records == null) {
            output.println(
                    "Unable to load missions."
            );
            return;
        }

        if (missionNumber > records.length) {
            output.println(
                    "Invalid mission number."
            );
            return;
        }

        String record =
                records[missionNumber - 1];

        displayRecord(
                record,
                "Mission Brief"
        );
    }

    /**
     * Displays the contents of a SearchResult.
     *
     * @param result result returned by MissionSearch
     */
    public void displaySearchResults(
            SearchResult result
    ) {

        if (result == null) {
            output.println(
                    "Search failed."
            );
            return;
        }

        String message =
                result.getMessage();

        if (
                message != null &&
                        !message.isBlank()
        ) {
            output.println(message);
        }

        List<String> matches =
                result.getMatches();

        if (
                matches == null ||
                        matches.isEmpty()
        ) {
            return;
        }

        output.println();
        output.println(
                "Search Results"
        );

        output.println(
                "--------------"
        );

        for (
                int i = 0;
                i < matches.size();
                i++
        ) {

            String record =
                    matches.get(i);

            String[] fields =
                    record.split(
                            "\t",
                            3
                    );

            if (fields.length < 3) {
                output.println(
                        (i + 1) +
                                ". " +
                                record
                );

                continue;
            }

            output.println(
                    (i + 1) +
                            ". " +
                            fields[0]
            );

            output.println(
                    "   Date: " +
                            fields[1]
            );

            output.println(
                    "   Brief: " +
                            fields[2]
            );
        }
    }

    /**
     * Prompts the user for a mission number.
     */
    private void readMissionSelection() {

        displayMissionList();

        output.print(
                "Enter mission number: "
        );

        if (!scanner.hasNextLine()) {
            return;
        }

        String input =
                scanner.nextLine().trim();

        try {

            int missionNumber =
                    Integer.parseInt(input);

            displayMission(
                    missionNumber
            );

        } catch (NumberFormatException e) {

            output.println(
                    "Invalid mission number."
            );
        }
    }

    /**
     * Prompts for a search word or phrase and displays
     * the result returned by MissionSearch.
     */
    private void runSearch() {

        output.print(
                "Enter search word or phrase: "
        );

        if (!scanner.hasNextLine()) {
            return;
        }

        String searchTerm =
                scanner.nextLine();

        SearchResult result =
                missionSearch.search(
                        searchTerm
                );

        displaySearchResults(
                result
        );
    }

    /**
     * Displays a tab-separated mission record.
     */
    private void displayRecord(
            String record,
            String heading
    ) {

        if (record == null) {
            output.println(
                    "Unable to display mission."
            );
            return;
        }

        String[] fields =
                record.split(
                        "\t",
                        3
                );

        if (fields.length < 3) {
            output.println(
                    "Unable to display mission."
            );
            return;
        }

        output.println();
        output.println(heading);

        output.println(
                "-".repeat(
                        heading.length()
                )
        );

        output.println(
                "Title: " +
                        fields[0]
        );

        output.println(
                "Date: " +
                        fields[1]
        );

        output.println(
                "Brief: " +
                        fields[2]
        );
    }

    /**
     * Creates MissionSearch for the three-argument constructor.
     */
    private static MissionSearch createMissionSearch(
            DatabaseManager databaseManager
    ) {

        if (databaseManager == null) {
            throw new IllegalArgumentException(
                    "DatabaseManager cannot be null."
            );
        }

        return new MissionSearch(
                databaseManager
        );
    }
}