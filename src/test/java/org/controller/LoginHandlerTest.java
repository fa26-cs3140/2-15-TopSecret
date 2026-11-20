package org.controller;

import org.managers.UserAccountManager;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LoginHandlerTest {

    @Test
    void parseFormDecodesValues() {
        Map<String, String> form = LoginHandler.parseForm("username=pat&password=Se%26cret+1");

        assertEquals("pat", form.get("username"));
        assertEquals("Se&cret 1", form.get("password"));
    }

    @Test
    void parseFormIgnoresMalformedPairs() {
        assertEquals(Map.of("a", "1"), LoginHandler.parseForm("a=1&broken&"));
    }

    @Test
    void constructorRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new LoginHandler(null, new SessionManager()));
        assertThrows(IllegalArgumentException.class, () -> new LoginHandler(mock(UserAccountManager.class), null));
    }
}
