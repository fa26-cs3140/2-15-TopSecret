package org.example;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserInterfaceTest {

    UserInterface tUser;

    /*
     * Homework 3 tests are preserved below.
     * Homework 4 test stubs are added afterward.
     */

    @Test
    void runInterface()
    {
        // Test 1: No arguments
        tUser = new UserInterface();
        String[] args_t1 = new String[0];
        assertDoesNotThrow(
                () -> tUser.runInterface(args_t1),
                "Test 1 Failed: Threw an exception"
        );

        // Test 2: One argument, valid
        String[] args_t2 = new String[1];
        args_t2[0] = "0";
        assertDoesNotThrow(
                () -> tUser.runInterface(args_t2),
                "Test 2 Failed: Threw an exception"
        );

        // Test 3: Two arguments, both valid
        String[] args_t3 = new String[2];
        args_t3[0] = "0";
        args_t3[1] = "0";
        assertDoesNotThrow(
                () -> tUser.runInterface(args_t3),
                "Test 3 Failed: Threw an exception"
        );

        // Test 4: One argument, invalid out of range
        String[] args_t4 = new String[1];
        args_t4[0] = "9";
        assertThrows(
                IllegalArgumentException.class,
                () -> tUser.runInterface(args_t4),
                "Test 4 Failed: Did not throw an exception"
        );

        // Test 5: One argument, invalid not a number
        String[] args_t5 = new String[1];
        args_t5[0] = "NotANumber";
        assertThrows(
                IllegalArgumentException.class,
                () -> tUser.runInterface(args_t5),
                "Test 5 Failed: Did not throw an exception"
        );

        // Test 6: Two arguments, second invalid out of range
        String[] args_t6 = new String[2];
        args_t6[0] = "0";
        args_t6[1] = "1000";
        assertThrows(
                IllegalArgumentException.class,
                () -> tUser.runInterface(args_t6),
                "Test 6 Failed: Did not throw an exception"
        );

        // Test 7: Two arguments, second invalid not a number
        String[] args_t7 = new String[2];
        args_t7[0] = "0";
        args_t7[1] = "NotANumber";
        assertThrows(
                IllegalArgumentException.class,
                () -> tUser.runInterface(args_t7),
                "Test 7 Failed: Did not throw an exception"
        );

        // Test 8: Two arguments, deciphered text
        String[] args_t8 = new String[2];
        args_t8[0] = "0";
        args_t8[1] = "1";
        assertDoesNotThrow(
                () -> tUser.runInterface(args_t8),
                "Test 8 Failed: Threw an exception"
        );
    }


    /*
     * Homework 4 Week 1 Test Stubs
     *
     * These tests define expected Homework 4 behavior.
     * They are disabled until the new interactive UserInterface
     * and its connections to the other components are implemented.
     */

    @Test
    @Disabled("Homework 4 Week 1 test stub")
    void displaysMenuOptions()
    {
        /*
         * Expected:
         * UserInterface displays options to:
         * 1. List missions
         * 2. Read a mission
         * 3. Search missions
         * 4. Exit
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 test stub")
    void displaysNumberedMissionList()
    {
        /*
         * Expected:
         * Mission titles are displayed with corresponding numbers.
         *
         * Example:
         * 1 Operation Sandtrap
         * 2 The Munich Lead
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 test stub")
    void validMissionSelectionDisplaysMission()
    {
        /*
         * Expected:
         * When the user selects a valid mission number,
         * the corresponding mission information is displayed.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 test stub")
    void invalidMissionSelectionIsHandled()
    {
        /*
         * Expected:
         * An invalid mission number produces an appropriate
         * response and does not terminate the program unexpectedly.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 test stub")
    void invalidMenuSelectionIsHandled()
    {
        /*
         * Expected:
         * Invalid menu input is handled gracefully and the
         * user is allowed to make another selection.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 test stub")
    void menuReturnsAfterMissionIsDisplayed()
    {
        /*
         * Expected:
         * After displaying a mission, UserInterface displays
         * the menu again rather than terminating.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 test stub")
    void exitSelectionEndsInterface()
    {
        /*
         * Expected:
         * Selecting the Exit option ends the interactive loop.
         */
    }

    @Test
    @Disabled("Homework 4 Week 1 test stub")
    void multipleActionsCanOccurInOneSession()
    {
        /*
         * Expected:
         * The user can list missions, read a mission,
         * and perform another action without restarting
         * the program.
         */
    }
}