package org.example;

import java.io.PrintStream;
import java.util.Scanner;

/**
 * Handles the interactive Homework 4 terminal interface.
 *
 * Mission data is requested through DatabaseManager.
 * This class does not directly execute SQL or access SQLite.
 */
public class UserInterface {

    private final DatabaseManager databaseManager;
    private final Scanner scanner;
    private final PrintStream output;

    /**
     * Creates a UserInterface using the normal program input,
     * output, and default mission database.
     */
    public UserInterface() {
        this(
                new DatabaseManager(),
                new Scanner(System.in),
                System.out
        );
    }

    /**
     * Creates a UserInterface using supplied dependencies.
     *
     * This constructor makes the class easier to test because
     * tests can provide their own database manager, input, and output.
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
        if (databaseManager == null) {
            throw new IllegalArgumentException(
                    "DatabaseManager cannot be null."
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

        this.databaseManager = databaseManager;
        this.scanner = scanner;
        this.output = output;
    }

    /**
     * Runs the interactive mission menu until the user selects Exit.
     */
    public void runInterface() {

        boolean running = true;

        while (running) {

            displayMenu();

            if (!scanner.hasNextLine()) {
                return;
            }

            String selection = scanner.nextLine().trim();

            switch (selection) {

                case "1":
                    displayMissionList();
                    break;

                case "2":
                    readMissionSelection();
                    break;

                case "3":
                    output.println("Exiting Top Secret.");
                    running = false;
                    break;

                default:
                    output.println(
                            "Invalid menu selection. Please try again."
                    );
                    break;
            }
        }
    }

    /**
     * Compatibility method for the existing TopSecret class.
     *
     * Homework 4 no longer uses mission-selection command-line
     * arguments, so the arguments are ignored and the interactive
     * interface is started.
     *
     * @param args command-line arguments
     */
    public void runInterface(String[] args) {
        runInterface();
    }

    /**
     * Displays the available menu options.
     */
    public void displayMenu() {

        output.println();
        output.println("Top Secret Mission Menu");
        output.println("-----------------------");
        output.println("1. List available mission briefs");
        output.println("2. Read a mission brief");
        output.println("3. Exit");
        output.print("Select an option: ");
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
        output.println("Available Mission Briefs");
        output.println("------------------------");

        for (int i = 0; i < titles.length; i++) {
            output.println(
                    (i + 1) + ". " + titles[i]
            );
        }
    }

    /**
     * Displays one mission selected using its displayed number.
     *
     * @param missionNumber one-based mission number
     */
    public void displayMission(int missionNumber) {

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

        if (
                missionNumber < 1 ||
                        missionNumber > records.length
        ) {
            output.println(
                    "Invalid mission number."
            );
            return;
        }

        String record =
                records[missionNumber - 1];

        String[] fields =
                record.split("\t", 3);

        if (fields.length < 3) {
            output.println(
                    "Unable to display mission."
            );
            return;
        }

        output.println();
        output.println("Mission Brief");
        output.println("-------------");
        output.println("Title: " + fields[0]);
        output.println("Date: " + fields[1]);
        output.println("Brief: " + fields[2]);
    }

    /**
     * Prompts the user to select a mission and handles
     * invalid number input.
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

            displayMission(missionNumber);

        } catch (NumberFormatException e) {

            output.println(
                    "Invalid mission number."
            );
        }
    }
}