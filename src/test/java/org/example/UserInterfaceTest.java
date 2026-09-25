package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserInterfaceTest {
    
    UserInterface tUser;
    
    @Test
    void runInterface() 
    {
        // Test 1: No arguments
        tUser = new UserInterface();
        String[] args_t1 = new String[0];
        assertDoesNotThrow(()->tUser.runInterface(args_t1), "Test 1 Failed: Threw an exception");
        
        // Test 2: One argument, valid
        String[] args_t2 = new String[1];
        args_t2[0] = "0";
        assertDoesNotThrow(()->tUser.runInterface(args_t2), "Test 2 Failed: Threw an exception");

        // Test 3: Two arguments, both valid
        String[] args_t3 = new String[2];
        args_t3[0] = "0";
        args_t3[1] = "0";
        assertDoesNotThrow(()->tUser.runInterface(args_t3), "Test 3 Failed: Thew an exception");

        // Test 4: One argument, invalid out of range
        String[] args_t4 = new String[1];
        args_t4[0] = "9";
        assertThrows(IllegalArgumentException.class, ()->tUser.runInterface(args_t4), "Test 4 Failed: Thew an exception");

        // Test 5: One argument, invalid not a number
        String[] args_t5 = new String[1];
        args_t5[0] = "NotANumber";
        assertThrows(IllegalArgumentException.class, ()->tUser.runInterface(args_t5), "Test 5 Failed: Did not an exception");

        // Test 6: Two arguments, second invalid out of range
        String[] args_t6 = new String[2];
        args_t6[0] = "0";
        args_t6[1] = "1000";
        assertThrows(IllegalArgumentException.class, ()->tUser.runInterface(args_t6), "Test 6 Failed: Did not throw an exception");

        // Test 7: Two arguments, second invalid not a number
        String[] args_t7 = new String[2];
        args_t7[0] = "0";
        args_t7[1] = "NotANumber";
        assertThrows(IllegalArgumentException.class, ()->tUser.runInterface(args_t7), "Test 7 Failed: Did not throw an exception");

        // Test 8: Two arguments, deciphered text
        String[] args_t8 = new String[2];
        args_t8[0] = "0";
        args_t8[1] = "1";
        assertDoesNotThrow(()->tUser.runInterface(args_t8), "Test 8 Failed: Thew an exception");
    }

}
