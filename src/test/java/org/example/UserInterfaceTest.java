package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInterfaceTest {

    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private MissionSearch missionSearch;

    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
    }

    private UserInterface createInterface(String input) {
        return new UserInterface(
                databaseManager,
                missionSearch,
                new Scanner(input),
                new PrintStream(output)
        );
    }

    @Test
    void displayMenuShowsRequiredOptions() {
        UserInterface ui = createInterface("");

        ui.displayMenu();

        String text = output.toString();

        assertTrue(text.contains("List available mission briefs"));
        assertTrue(text.contains("Read a mission brief"));
        assertTrue(text.contains("Search mission briefs"));
        assertTrue(text.contains("Exit"));
    }

    @Test
    void displayMissionListShowsNumberedTitles() {
        when(databaseManager.getColumn("Title"))
                .thenReturn(new String[]{
                        "Operation Sandtrap",
                        "The Munich Lead",
                        "Project Bluebird"
                });

        UserInterface ui = createInterface("");

        ui.displayMissionList();

        String text = output.toString();

        assertTrue(text.contains("1. Operation Sandtrap"));
        assertTrue(text.contains("2. The Munich Lead"));
        assertTrue(text.contains("3. Project Bluebird"));

        verify(databaseManager).getColumn("Title");
    }

    @Test
    void displayMissionShowsSelectedMission() {
        when(databaseManager.getRecords("Title", "%"))
                .thenReturn(new String[]{
                        "Operation Sandtrap\t1970-11-03\tBug the diplomatic pouch.",
                        "The Munich Lead\t1972-09-15\tIdentify the logistical backbone."
                });

        UserInterface ui = createInterface("");

        ui.displayMission(2);

        String text = output.toString();

        assertTrue(text.contains("The Munich Lead"));
        assertTrue(text.contains("1972-09-15"));
        assertTrue(text.contains("Identify the logistical backbone."));

        verify(databaseManager).getRecords("Title", "%");
    }

    @Test
    void displayMissionRejectsNumberThatIsTooSmall() {
        UserInterface ui = createInterface("");

        ui.displayMission(0);

        assertTrue(
                output.toString().contains("Invalid mission number")
        );

        verifyNoInteractions(databaseManager);
    }

    @Test
    void displayMissionRejectsNumberThatIsTooLarge() {
        when(databaseManager.getRecords("Title", "%"))
                .thenReturn(new String[]{
                        "Operation Sandtrap\t1970-11-03\tTest mission."
                });

        UserInterface ui = createInterface("");

        ui.displayMission(5);

        assertTrue(
                output.toString().contains("Invalid mission number")
        );
    }

    @Test
    void runInterfaceCanListThenExit() {
        when(databaseManager.getColumn("Title"))
                .thenReturn(new String[]{
                        "Operation Sandtrap",
                        "The Munich Lead"
                });

        UserInterface ui = createInterface(
                "1\n4\n"
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("1. Operation Sandtrap"));
        assertTrue(text.contains("2. The Munich Lead"));
        assertTrue(text.contains("Exiting"));
    }

    @Test
    void runInterfaceCanReadMissionThenReturnToMenu() {
        when(databaseManager.getColumn("Title"))
                .thenReturn(new String[]{
                        "Operation Sandtrap"
                });

        when(databaseManager.getRecords("Title", "%"))
                .thenReturn(new String[]{
                        "Operation Sandtrap\t1970-11-03\tBug the diplomatic pouch."
                });

        UserInterface ui = createInterface(
                "2\n1\n4\n"
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("Operation Sandtrap"));
        assertTrue(text.contains("1970-11-03"));

        assertTrue(
                countOccurrences(
                        text,
                        "List available mission briefs"
                ) >= 2
        );
    }

    @Test
    void searchOptionDisplaysMatchingMissions() {
        String record =
                "Operation Sandtrap\t1970-11-03\tBug the diplomatic pouch.";

        when(missionSearch.search("diplomatic"))
                .thenReturn(
                        new SearchResult(
                                List.of(record),
                                null
                        )
                );

        UserInterface ui = createInterface(
                "3\ndiplomatic\n4\n"
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("Search Results"));
        assertTrue(text.contains("Operation Sandtrap"));
        assertTrue(text.contains("1970-11-03"));
        assertTrue(text.contains("Bug the diplomatic pouch."));

        verify(missionSearch).search("diplomatic");
    }

    @Test
    void searchOptionDisplaysNoMatchesMessage() {
        when(missionSearch.search("missing"))
                .thenReturn(
                        new SearchResult(
                                List.of(),
                                "No matches were found."
                        )
                );

        UserInterface ui = createInterface(
                "3\nmissing\n4\n"
        );

        ui.runInterface();

        assertTrue(
                output.toString().contains(
                        "No matches were found."
                )
        );

        verify(missionSearch).search("missing");
    }

    @Test
    void searchFailureIsHandledGracefully() {
        when(missionSearch.search("agent"))
                .thenReturn(
                        new SearchResult(
                                null,
                                "Search failed."
                        )
                );

        UserInterface ui = createInterface(
                "3\nagent\n4\n"
        );

        ui.runInterface();

        assertTrue(
                output.toString().contains("Search failed.")
        );
    }

    @Test
    void invalidMenuInputDoesNotEndProgram() {
        UserInterface ui = createInterface(
                "hello\n4\n"
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("Invalid menu selection"));
        assertTrue(text.contains("Exiting"));
    }

    @Test
    void invalidMissionInputDoesNotEndProgram() {
        when(databaseManager.getColumn("Title"))
                .thenReturn(new String[]{
                        "Operation Sandtrap"
                });

        UserInterface ui = createInterface(
                "2\nhello\n4\n"
        );

        ui.runInterface();

        String text = output.toString();

        assertTrue(text.contains("Invalid mission number"));
        assertTrue(text.contains("Exiting"));
    }

    @Test
    void missingMissionListIsHandledGracefully() {
        when(databaseManager.getColumn("Title"))
                .thenReturn(null);

        UserInterface ui = createInterface("");

        ui.displayMissionList();

        assertTrue(
                output.toString().contains(
                        "Unable to load missions"
                )
        );
    }

    @Test
    void nullDatabaseManagerIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UserInterface(
                        null,
                        missionSearch,
                        new Scanner(""),
                        new PrintStream(output)
                )
        );
    }

    @Test
    void nullMissionSearchIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UserInterface(
                        databaseManager,
                        null,
                        new Scanner(""),
                        new PrintStream(output)
                )
        );
    }

    @Test
    void nullScannerIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UserInterface(
                        databaseManager,
                        missionSearch,
                        null,
                        new PrintStream(output)
                )
        );
    }

    @Test
    void nullOutputIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new UserInterface(
                        databaseManager,
                        missionSearch,
                        new Scanner(""),
                        null
                )
        );
    }

    private int countOccurrences(
            String text,
            String target
    ) {
        int count = 0;
        int index = 0;

        while (
                (index = text.indexOf(target, index)) != -1
        ) {
            count++;
            index += target.length();
        }

        return count;
    }
}