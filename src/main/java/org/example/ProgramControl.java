package org.example;

import java.util.List;

//Controls the main application logic by connecting the UserInterface, FileHandler, and Cipher classes.
public class ProgramControl {

    private final FileHandler fileHandler;

    //Creates a ProgramControl using the default data directory.

    public ProgramControl() {
        this.fileHandler = new FileHandler();
    }

    //@param fileHandler FileHandler used to access files
    public ProgramControl(FileHandler fileHandler) {
        if (fileHandler == null) {
            throw new IllegalArgumentException("FileHandler cannot be null.");
        }

        this.fileHandler = fileHandler;
    }

    //@return list of available filenames
    public List<String> getAvailableFiles() {
        return fileHandler.getAvailableFiles();
    }

    /**
     * Reads and deciphers the selected file using the default key.
     *
     * The first line of key.txt is the original character mapping.
     * Using that same mapping as both lines creates the default
     * identity key.
     *
     * @param filename file to read
     * @return readable file contents, or null if the file cannot be read
     */
    public String getFileContents(String filename) {
        String keyContents = fileHandler.readFile("key.txt");

        if (keyContents == null) {
            return null;
        }

        String[] keyLines = keyContents.split("\\R");

        if (keyLines.length == 0) {
            return null;
        }

        String mapping = keyLines[0];

        return getFileContentsWithKey(filename, mapping + "\n" + mapping);
    }

    //Reads and deciphers the selected file using a particular key mapping from key.txt.
    //@param filename file to read
    //@param keyIndex index of the cipher mapping in key.txt
    // @return deciphered contents, or null if the file/key cannot be read
    public String getFileContents(String filename, int keyIndex) {
        String keyContents = fileHandler.readFile("key.txt");

        if (keyContents == null) {
            return null;
        }

        String[] keyLines = keyContents.split("\\R");

        if (keyLines.length == 0 ||
                keyIndex < 0 ||
                keyIndex >= keyLines.length) {
            throw new IllegalArgumentException("Invalid cipher key index.");
        }

        String mapping = keyLines[0];
        String cipherMapping = keyLines[keyIndex];

        return getFileContentsWithKey(
                filename,
                mapping + "\n" + cipherMapping
        );
    }

    // Reads a file and deciphers it using the supplied key.
    private String getFileContentsWithKey(String filename, String key) {
        String cipheredContents = fileHandler.readFile(filename);

        if (cipheredContents == null) {
            return null;
        }

        CipherKey cipherKey = new CipherKey(key);
        Cipher cipher = new Cipher(cipherKey);

        return cipher.decipher(cipheredContents);
    }
}
