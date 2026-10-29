package org.example;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.managers.*;

//Controls the main application logic by connecting the UserInterface, FileHandler, and Cipher classes.
public class ProgramControl {

    private final FileHandler fileHandler;
    private final BriefManager briefManager;
    private final AgentManager agentManager;
    private final FacilityManager facilityManager;
    private final AuditLogManager auditLogger;
    private final String databaseFile = "mission_database.db";
    private final String databaseURL = "jdbc:sqlite:data/" + databaseFile;

    //Creates a ProgramControl using the default data directory.

    public ProgramControl() {
        this.fileHandler = new FileHandler();
        this.briefManager = new BriefManager(this.databaseURL);
        this.agentManager = new AgentManager(this.databaseURL);
        this.facilityManager = new FacilityManager(this.databaseURL);
        this.auditLogger = new AuditLogManager(this.databaseURL);
    }

    /**
     * Runs the main program, handles the creation of a database
     *
     * @param ui UserInterface to be passed
     * @param args Arguments to pasesed into ui.runInterface()
     */
    public void run(String[] args) 
    {
        // We are going to now create a table that contains for this
        // TODO: Refactor DatabaseManager for this
        // The database will now just be called "mission_database"
        // and it will contain a table for all mission information
        // Information in the database should be ciphered

        // Ensure that the file already exists, if not create it.
        if (!this.getAvailableFiles().contains(databaseFile))
        {
            fileHandler.writeFile(databaseFile, "");
        }

        // Then import the data from the relevant .tsv file as a .db
        this.createModelTables();

        // Check to see if users exist. If not, prompt them.
        // // TODO: Implement proper user management
        AgencyInterface ui = 
            new AgencyInterface(this.agentManager, this.facilityManager, this.briefManager, 
                                this.auditLogger, new UserAccount("test", true), new Scanner(System.in), System.out);

        ui.run();
    }

    // End of run()  

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

    private void createModelTables()
    {

        try
        {
            if (!this.briefManager.tableExists("briefs"))
            {
                this.briefManager.importTSV(fileHandler.getDataDirectory() + "/mission_briefs.tsv",
                                               "briefs",
                                               fileHandler.getDataDirectory(), 
                                               databaseFile);
            }
            if (!this.facilityManager.tableExists("facilities"))
            {
                this.facilityManager.importTSV(fileHandler.getDataDirectory() + "/mission_briefs.tsv",
                                               "briefs",
                                               fileHandler.getDataDirectory(), 
                                               databaseFile);
            }
            if (!this.agentManager.tableExists("agents"))
            {
                this.agentManager.importTSV(fileHandler.getDataDirectory() + "/mission_briefs.tsv",
                                               "briefs",
                                               fileHandler.getDataDirectory(), 
                                               databaseFile);
            }
        }
        catch (IOException e)
        {
            DebugHelper.debugPrintln(e.getMessage());
        }
    }
}
