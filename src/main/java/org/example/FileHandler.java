package org.example;

import java.util.ArrayList;
import java.util.List;

public class FileHandler {
    private String dataDirectory;

    /**
     * Creates a FileHandler that uses the "data" directory by default.
     */
    public FileHandler() {
        this.dataDirectory = "data";
    }

    /**
     * Creates a FileHandler that uses the specified data directory.
     *
     * @param dataDirectory the directory containing the data files
     */
    public FileHandler(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    /**
     * Returns the current data directory.
     *
     * @return the data directory
     */
    public String getDataDirectory() {
        return dataDirectory;
    }

    /**
     * Sets the directory used to access data files.
     *
     * @param dataDirectory the new data directory
     */
    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    /**
     * Returns the names of all available files in the data directory.
     *
     * @return a list of available filenames
     */
    public List<String> getAvailableFiles() {
        // TODO
        return new ArrayList<>();
    }

    /**
     * Read and returns the contents of the requested file
     *
     * @param filename the name of the requested file
     * @return the file contents, or null if the file cannot be read
     */
    public String readFile(String filename) {
        // TODO
        return null;
    }
}