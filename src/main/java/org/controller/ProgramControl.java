package org.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.managers.*;
import org.model.*;
import org.view.AgencyInterface;
import org.managers.FileHandler;

//Controls the main application logic by connecting the UserInterface, FileHandler, and Cipher classes.
public class ProgramControl {

    private final FileHandler fileHandler;
    private final BriefManager briefManager;
    private final AgentManager agentManager;
    private final FacilityManager facilityManager;
    private final MissionManager missionManager;
    private final AuditLogManager auditLogger;
    private final UserAccountManager userManager;
    private final String databaseFile = "mission_database.db";
    private final String databaseURL = "jdbc:sqlite:data/" + databaseFile;

    //Creates a ProgramControl using the default data directory.

    public ProgramControl() {
        this.fileHandler = new FileHandler();
        this.briefManager = new BriefManager(this.databaseURL);
        this.agentManager = new AgentManager(this.databaseURL);
        this.facilityManager = new FacilityManager(this.databaseURL);
        this.missionManager = new MissionManager(this.databaseURL);
        this.auditLogger = new AuditLogManager(this.databaseURL);

        // TODO: Figure out how cipher works
        String[] stringKeys = fileHandler.readFile("key.txt").split("\n");
        CipherKey cipKey = new CipherKey(stringKeys[1] + "\n" + stringKeys[0]);
        Cipher cipher = new Cipher(cipKey);
        this.userManager = new UserAccountManager(this.databaseURL, fileHandler, cipher);
    }


    ProgramControl(FileHandler fileHandler, BriefManager briefManager, AgentManager agentManager, FacilityManager facilityManager, AuditLogManager auditLogger, UserAccountManager userManager, MissionManager missionManager) {
        if (fileHandler == null || briefManager == null || agentManager == null || facilityManager == null || auditLogger == null || userManager == null) {
            throw new IllegalArgumentException("Dependencies cannot be null.");
        }

        this.fileHandler = fileHandler;
        this.briefManager = briefManager;
        this.agentManager = agentManager;
        this.facilityManager = facilityManager;
        this.auditLogger = auditLogger;
        this.userManager = userManager;
        this.missionManager = missionManager;
    }

    /**
     * Runs the main program, handles the creation of a database
     *
     * @param args Arguments to pasesed into ui.runInterface()
     */
    public void run(String[] args) 
    {
        // We are going to now create a table that contains for this
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

        // -web starts the read-only web view instead of the terminal menu
        if (LaunchOptions.isWebMode(args))
        {
            startWeb(WebServer.DEFAULT_PORT);
            return;
        }

        AgencyInterface ui =
            new AgencyInterface(this.agentManager, this.facilityManager, this.briefManager, this.userManager, this.missionManager,
                                this.auditLogger, new UserAccount("__DUMMY__", false), new Scanner(System.in), System.out);

        if (!userManager.hasUsers())
        {
            // Prompt the user to create an account that will automatically be
            // an admin
            String[] data = ui.userCreationPrompt();
            UserAccount user = userManager.createFirstUser(data[0], data[1]);
            ui.setUser(user);
        }
        else
        {
            boolean loggedIn = false;
            // Prompt the user to select an account OR to create a new account
            while (!loggedIn)
            {
                String[] data = ui.userSelectionPrompt();
                if (data == null) return;
                String username = data[0];
                String password = data[1];

                if (userManager.verifyLogin(username, password))
                {
                    UserAccount user = userManager.getUser(username);
                    ui.setUser(user);
                    // Log the login event
                    auditLogger.log(user.getUsername(), "LOGIN", "LOGIN", "x");
                    loggedIn = true;
                }
                else
                {
                    ui.message("Invalid account information.");
                    ui.message("Try again or enter empty username to exit.");
                }
            }
        }

        // --change-password changes the password right after login
        if (Arrays.asList(args).contains("--change-password"))
        {
            ui.changePasswordMenu();
        }

        ui.run();
}

    // End of run()

    // Starts the web view on the port; returns null (with a message) when it cannot start,
    // for example before the first account has been created in the terminal.
    WebServer startWeb(int port)
    {
        AppController controller = new AppController(briefManager, agentManager, missionManager, facilityManager, auditLogger);
        WebServer server = new WebServer(controller, userManager, new SessionManager(), port);
        try
        {
            server.start();
        }
        catch (IllegalStateException | IOException e)
        {
            System.out.println("Web view not started: " + e.getMessage());
            return null;
        }
        System.out.println("Web view running at http://localhost:" + port + "/ (press Ctrl+C to stop)");
        return server;
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

    // Creates model tables if they do not exist
    private void createModelTables()
    {
        briefManager.createTable();
        agentManager.createTable();
        facilityManager.createTable();
        missionManager.createTable();
    }
}
