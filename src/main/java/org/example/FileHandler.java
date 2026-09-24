package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all direct access to data files.
 * Provides methods for listing available files and reading file contents.
 */
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
     * @return a list of available filenames or an empty list if the
     * directory cannot be accessed
     */
    public List<String> getAvailableFiles() {
        // implementation inspired by GeeksForGeeks:
        // https://www.geeksforgeeks.org/java/how-to-list-all-files-in-a-directory-in-java/
        File directory = new File(dataDirectory);
        File[] allFiles = directory.listFiles();

        ArrayList<String> fileList = new ArrayList<>();

        if (allFiles != null) {
            for (File file : allFiles) {
                if (file.isFile())
                    fileList.add(file.getName());
            }
        }

        return fileList;
    }

    /**
     * Read and returns the contents of the requested file
     *
     * @param filename the name of the requested file
     * @return the file contents, or null if the file cannot be read
     */
    public String readFile(String filename) {
        // Similar implementation to Professor Stone's video
        String returnString = "";
        File targetFile = new File(dataDirectory, filename);

        try (BufferedReader reader = new BufferedReader(new FileReader(targetFile))) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                returnString += line + "\n";
            }
        } catch (IOException e) {
            return null;
        }

        return returnString.strip();
    }
}