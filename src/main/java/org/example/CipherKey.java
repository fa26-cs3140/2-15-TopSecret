
package org.example;

/**
 * Validates and stores the character mappings used by the cipher.
 * The first line contains the original characters, and the second
 * line contains their encrypted equivalents.
 */
public class CipherKey {

    private final char[] original;
    private final char[] ciphered;

    /**
     * Constructs a CipherKey from the contents of a key file.
     *
     * @param key the contents of the key file
     * @throws IllegalArgumentException if the key is invalid
     */
    public CipherKey(String key) {

        if (!validateKey(key)) {
            throw new IllegalArgumentException("Invalid cipher key.");
        }

        String[] lines = key.split("\\R", -1);

        original = lines[0].toCharArray();
        ciphered = lines[1].toCharArray();
    }

    /**
     * Checks that the key contains two lines of equal length
     * and that each character has a unique mapping.
     *
     * @param key the key file contents
     * @return true if the key is valid, false otherwise
     */
    public boolean validateKey(String key) {

        if (key == null || key.isEmpty()) {
            return false;
        }

        String[] lines = key.split("\\R", -1);

        // Allow a single trailing newline at the end of the file.
        if (lines.length == 3 && lines[2].isEmpty()) {
            lines = new String[]{lines[0], lines[1]};
        }

        if (lines.length != 2) {
            return false;
        }

        String first = lines[0];
        String second = lines[1];

        // Both lines must contain the same number of characters.
        if (first.isEmpty() || first.length() != second.length()) {
            return false;
        }

        // Each original and encrypted character must be unique.
        for (int i = 0; i < first.length(); i++) {

            for (int j = 0; j < i; j++) {

                if (first.charAt(i) == first.charAt(j)
                        || second.charAt(i) == second.charAt(j)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns the original characters.
     *
     * @return a copy of the original character array
     */
    public char[] getOriginal() {
        return original.clone();
    }

    /**
     * Returns the encrypted characters.
     *
     * @return a copy of the encrypted character array
     */
    public char[] getCiphered() {
        return ciphered.clone();
    }
}