package org.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LaunchOptionsTest {

    @Test
    void webFlagStartsWebMode() {
        assertTrue(LaunchOptions.isWebMode(new String[]{"-web"}));
    }

    @Test
    void webFlagIsFoundAmongOtherArguments() {
        assertTrue(LaunchOptions.isWebMode(new String[]{"--change-password", "-web"}));
    }

    @Test
    void noArgumentsStartsTerminalMode() {
        assertFalse(LaunchOptions.isWebMode(new String[0]));
    }

    @Test
    void nullArgumentsStartTerminalMode() {
        assertFalse(LaunchOptions.isWebMode(null));
    }

    @Test
    void otherArgumentsStartTerminalMode() {
        assertFalse(LaunchOptions.isWebMode(new String[]{"web", "--web", "-WEB"}));
    }
}
