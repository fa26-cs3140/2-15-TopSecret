package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CipherTest {

    private static final String VALID_KEY =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890\n"
                    + "bcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890a";

    @Test
    void testBasicDecipher() {
        Cipher cipher = new Cipher(new CipherKey(VALID_KEY));

        assertEquals("abc", cipher.decipher("bcd"));
    }

    @Test
    void testAlphabetWraparound() {
        Cipher cipher = new Cipher(new CipherKey(VALID_KEY));

        assertEquals("z", cipher.decipher("A"));
        assertEquals("Z", cipher.decipher("1"));
        assertEquals("0", cipher.decipher("a"));
    }

    @Test
    void testNumbers() {
        Cipher cipher = new Cipher(new CipherKey(VALID_KEY));

        assertEquals("123", cipher.decipher("234"));
        assertEquals("7890", cipher.decipher("890a"));
    }

    @Test
    void testUnmappedCharacters() {
        Cipher cipher = new Cipher(new CipherKey(VALID_KEY));

        assertEquals("Hello, World!",
                cipher.decipher("Ifmmp, Xpsme!"));
    }

    @Test
    void testEmptyString() {
        Cipher cipher = new Cipher(new CipherKey(VALID_KEY));

        assertEquals("", cipher.decipher(""));
    }

    @Test
    void testRepeatedCharacters() {
        Cipher cipher = new Cipher(new CipherKey(VALID_KEY));

        assertEquals("aaa", cipher.decipher("bbb"));
    }

    @Test
    void testNullCipherKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cipher(null));
    }

    @Test
    void testNullCipheredText() {
        Cipher cipher = new Cipher(new CipherKey(VALID_KEY));

        assertThrows(IllegalArgumentException.class,
                () -> cipher.decipher(null));
    }

    @Test
    void testAlternateKey() {
        String alternateKey = "abc\nxyz";

        Cipher cipher = new Cipher(new CipherKey(alternateKey));

        assertEquals("cab", cipher.decipher("zxy"));
    }

    @Test
    void testCharactersOutsideAlternateKey() {
        String alternateKey = "abc\nxyz";

        Cipher cipher = new Cipher(new CipherKey(alternateKey));

        assertEquals("cab! 123",
                cipher.decipher("zxy! 123"));
    }

    @Test
    void testPreserveNewlines() {
        Cipher cipher = new Cipher(new CipherKey(VALID_KEY));

        assertEquals("abc\ndef",
                cipher.decipher("bcd\nefg"));
    }
}