package org.example;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class UserInterfaceIntegrationTest {

    /*
     * Homework 4 Week 1 Integration Test Definitions
     *
     * These tests describe how UserInterface must interact
     * with the components owned by the other team members.
     *
     * The tests are disabled until those components and their
     * interfaces are finalized.
     */

    @Test
    @Disabled("Homework 4 Week 1 integration test stub")
    void successfulLoginOpensMissionInterface()
    {
        /*
         * Components:
         * Team Member B authentication -> Team Member D UserInterface
         *
         * Expected:
         * After valid credentials are accepted,
         * control is transferred to the mission menu.
         *
         * The mission menu should not be displayed before
         * authentication succeeds.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 integration test stub")
    void userInterfaceDisplaysMissionsFromDataLayer()
    {
        /*
         * Components:
         * Team Member D UserInterface -> Team Member A data component
         *
         * Expected:
         * Mission records stored in SQLite are made available
         * through the agreed-upon interface.
         *
         * UserInterface displays the returned mission titles
         * as a numbered list.
         *
         * UserInterface must not execute SQL directly.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 integration test stub")
    void selectedMissionDisplaysDatabaseRecord()
    {
        /*
         * Components:
         * UserInterface -> mission data component
         *
         * Expected:
         * Selecting a mission number requests the corresponding
         * mission record and displays its information.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 integration test stub")
    void searchResultsAreDisplayedByUserInterface()
    {
        /*
         * Components:
         * Team Member D UserInterface -> Team Member C search feature
         *
         * Expected:
         * The user selects Search and enters a word or phrase.
         * Search results returned by Team Member C's component
         * can then be displayed by UserInterface.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 integration test stub")
    void noSearchMatchesCanBeDisplayed()
    {
        /*
         * Components:
         * Team Member C search feature -> Team Member D UserInterface
         *
         * Expected:
         * If the search component reports that no missions matched,
         * UserInterface displays an appropriate no-matches message.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 integration test stub")
    void multipleDatabaseRequestsCanOccurInOneSession()
    {
        /*
         * Components:
         * UserInterface -> mission data component
         *
         * Expected:
         * A user may list missions, read a mission,
         * return to the menu, and request another mission
         * without restarting the application.
         */
    }
}