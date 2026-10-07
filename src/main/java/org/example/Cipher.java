
package org.example;

/**
 * Deciphers encrypted text using a validated CipherKey.
 */
public class Cipher {

    private final CipherKey cipherKey;

    /**
     * Constructs a Cipher using the supplied key.
     *
     * @param cipherKey the validated cipher key
     */
    public Cipher(CipherKey cipherKey) {

        if (cipherKey == null) {
            throw new IllegalArgumentException(
                    "Cipher key cannot be null."
            );
        }

        this.cipherKey = cipherKey;
    }
    public String cipher(String text) {

        if (text == null) {
            throw new IllegalArgumentException(
                    "Text cannot be null."
            );
        }

        char[] original = cipherKey.getOriginal();
        char[] ciphered = cipherKey.getCiphered();

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {

            char current = text.charAt(i);
            char encoded = current;

            for (int j = 0; j < original.length; j++) {

                if (current == original[j]) {

                    // Replace it with the CIPHERED character
                    encoded = ciphered[j];
                    break;
                }
            }

            result.append(encoded);
        }

        return result.toString();
    }

    /**
     * Deciphers the provided text using the character mappings
     * stored in CipherKey.
     *
     * @param cipheredText the encrypted text
     * @return the deciphered text
     */
    public String decipher(String cipheredText) {

        if (cipheredText == null) {
            throw new IllegalArgumentException(
                    "Ciphered text cannot be null."
            );
        }

        char[] original = cipherKey.getOriginal();
        char[] ciphered = cipherKey.getCiphered();

        StringBuilder result = new StringBuilder();

        // Iterate through every character in the encrypted text.
        for (int i = 0; i < cipheredText.length(); i++) {

            char current = cipheredText.charAt(i);
            char decoded = current;

            // Find the encrypted character in the cipher key.
            for (int j = 0; j < ciphered.length; j++) {

                if (current == ciphered[j]) {

                    // Replace it with the original character
                    // at the corresponding index.
                    decoded = original[j];
                    break;
                }
            }

            // Characters not in the key remain unchanged.
            result.append(decoded);
        }

        return result.toString();
    }
}