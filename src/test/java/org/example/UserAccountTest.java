package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAccountTest {

    @Test
    void constructorAndGettersStoreAccountData() {

        UserAccount account =
                new UserAccount(
                        "agent_7",
                        false
                );

        assertEquals(
                "agent_7",
                account.getUsername()
        );

        assertFalse(
                account.isAdmin()
        );
    }

    @Test
    void settersUpdateAccountData() {

        UserAccount account =
                new UserAccount(
                        "agent_7",
                        false
                );

        account.setUsername(
                "admin-user"
        );

        account.setAdmin(
                true
        );

        assertEquals(
                "admin-user",
                account.getUsername()
        );

        assertTrue(
                account.isAdmin()
        );
    }
}