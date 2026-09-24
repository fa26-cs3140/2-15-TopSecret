package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CipherKeyTest {

    // The sample key provided with the assignment.
    private static final String VALID_KEY =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890\n"
                    + "bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890a";

    @Test
    void testValidKey() {
        CipherKey key = new CipherKey(VALID_KEY);

        assertTrue(key.validateKey(VALID_KEY));
    }

    @Test
    void testCharacterArrays() {
        CipherKey key = new CipherKey(VALID_KEY);

        char[] original = key.getOriginal();
        char[] ciphered = key.getCiphered();

        assertEquals(original.length, ciphered.length);

        assertEquals('a', original[0]);
        assertEquals('b', ciphered[0]);

        assertEquals('0', original[original.length - 1]);
        assertEquals('a', ciphered[ciphered.length - 1]);
    }

    @Test
    void testInvalidKeyDifferentLengths() {
        String invalidKey = "ABC\nXY";

        assertThrows(IllegalArgumentException.class,
                () -> new CipherKey(invalidKey));
    }

    @Test
    void testInvalidKeyDuplicateCharacters() {
        String invalidKey = "ABC\nXXY";

        assertThrows(IllegalArgumentException.class,
                () -> new CipherKey(invalidKey));
    }

    @Test
    void testInvalidKeyMissingLine() {
        String invalidKey = "ABC";

        assertThrows(IllegalArgumentException.class,
                () -> new CipherKey(invalidKey));
    }

    @Test
    void testInvalidKeyNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new CipherKey(null));
    }

    @Test
    void testEmptyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new CipherKey(""));
    }

    @Test
    void testTrailingNewline() {
        CipherKey key = new CipherKey(VALID_KEY + "\n");

        assertTrue(key.validateKey(VALID_KEY + "\n"));
    }

    @Test
    void testInvalidKeyAdditionalLine() {
        String invalidKey = "ABC\nXYZ\n123";

        assertFalse(new CipherKey(VALID_KEY).validateKey(invalidKey));
    }

    @Test
    void testArrayModificationDoesNotAffectKey() {
        CipherKey key = new CipherKey(VALID_KEY);

        char[] original = key.getOriginal();
        original[0] = 'Z';

        assertEquals('a', key.getOriginal()[0]);
    }

    @Test
    void testEmptyLinesInvalid() {
        assertFalse(new CipherKey(VALID_KEY).validateKey("\n"));
    }
}