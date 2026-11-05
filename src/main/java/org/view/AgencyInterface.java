package org.view;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Predicate;

import org.controller.AppController;
import org.managers.AgentManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.managers.UserAccountManager;
import org.managers.AuditLogService;
import org.model.*;

public class AgencyInterface {

    // private final AgencyDataService data;
    private final AgentManager aManager;
    private final FacilityManager fManager;
    private final BriefManager bManager;
    private final UserAccountManager uManager;
    private final MissionManager mManager;
    private final AuditLogService auditLog;
    private UserAccount currentUser;
    private final Scanner scanner;
    private final PrintStream output;
    private final AppController controller;

    private final String SEPARATOR = "-----------------";

    public AgencyInterface(AgentManager aManager, FacilityManager fManager, BriefManager bManager, UserAccountManager uManager, MissionManager mManager,
                           AuditLogService auditLog, UserAccount currentUser, Scanner scanner, PrintStream output) {
        if (aManager == null || fManager == null || bManager == null || auditLog == null || currentUser == null || scanner == null || output == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        // this.data = data;
        this.aManager = aManager;
        this.fManager = fManager;
        this.bManager = bManager;
        this.uManager = uManager;
        this.mManager = mManager;
        this.auditLog = auditLog;
        this.currentUser = currentUser;
        this.scanner = scanner;
        this.output = output;
        this.controller = new AppController(bManager, aManager, mManager, fManager, auditLog);
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }

    public void run() {
        try {
            while (true) {
                displayMainMenu();
                switch (readLine()) {
                    case "1" -> runTypeMenu(RecordType.BRIEF);
                    case "2" -> runTypeMenu(RecordType.AGENT);
                    case "3" -> runTypeMenu(RecordType.FACILITY);
                    case "4" -> runTypeMenu(RecordType.MISSION);
                    case "5" -> userCreationMenu();
                    case "6" -> { if (isAdmin()) displayAuditLog(); else invalidSelection(); }
                    case "7" -> { if (isAdmin()) userPromotionMenu(); else invalidSelection(); }
                    case "8" -> changePasswordMenu();
                    case "0" -> { exit(); return; } // Make sure to run exit() to log logut info
                    default -> invalidSelection();
                }
            }
        } catch (NoSuchElementException e) {
            // Input ran out; stop quietly.
        }
    }

    public void setUser(UserAccount user)
    {
        this.currentUser = user;
    }

    public void displayMainMenu() {
        output.println("\nTop Secret Main Menu\n1. Briefs\n2. Agents\n3. Facilities\n4. Create User");
        if (isAdmin()) {
            output.println("5. Review audit log");
            output.println("6. Manage User Permissions");
        }
        output.println("7. Change Password");
        output.print("0. Exit\nSelect an option: ");
    }

    public void displayTypeMenu(RecordType type) {
        output.println("\n" + type.getTitle() + "\n1. List\n2. Read");
        if (isAdmin()) {
            output.println("3. Add");
            // Cannot import missions
            if (type != RecordType.MISSION) output.println("4. Import");
        }
        output.print("0. Back\nSelect an option: ");
    }

    public String[] userCreationPrompt()
    {
        output.println(this.SEPARATOR);
        output.println("User Creation Menu");
        String username = promptUntilValid("Username: ", uManager::isValidUsername,
                "Usernames need 3+ lowercase letters, numbers, hyphens, or underscores.");
        String password = promptNewPassword();
        output.println(this.SEPARATOR);

        return new String[] { username, password };
    }

    // Asks for the current password before saving a new one. Returns true if the password changed.
    public boolean changePasswordMenu()
    {
        output.println(this.SEPARATOR);
        output.print("Current password: ");
        if (!uManager.verifyLogin(currentUser.getUsername(), readLine()))
        {
            output.println("Incorrect password. Password not changed.");
            output.println(this.SEPARATOR);
            return false;
        }
        boolean changed = uManager.changePassword(currentUser.getUsername(), promptNewPassword());
        output.println(changed ? "Password changed." : "Unable to change password.");
        output.println(this.SEPARATOR);
        return changed;
    }

    private String promptNewPassword()
    {
        return promptUntilValid("Password: ", uManager::isValidPassword,
                "Passwords need 5+ characters with uppercase and lowercase letters.");
    }

    private String promptUntilValid(String label, Predicate<String> isValid, String rules)
    {
        while (true)
        {
            String value = prompt(label);
            if (isValid.test(value)) return value;
            output.println(rules);
        }
    }

    private void exit()
    {
        auditLog.log(currentUser.getUsername(), "LOGOUT", "LOGOUT", "x");
        output.println("Exiting Top Secret.");
    }

    private void userCreationMenu()
    {
        output.println(this.SEPARATOR);
        output.println("Create new user account");
        String[] data = this.userCreationPrompt();
        try
        {
            uManager.createUser(data[0], false);
        }
        catch (IllegalArgumentException e)
        {
            output.println("Unable to create user: " + e.getMessage());
            output.println(this.SEPARATOR);
            return;
        }
        uManager.createCredentials(data[0], data[1]);
        auditLog.log(currentUser.getUsername(), "CREATE", "USER", data[0]);
        output.println(this.SEPARATOR);
    }

    private void userPromotionMenu()
    {
        output.println(this.SEPARATOR);
        output.println("Grant or revoke administrative privelages");
        output.println(this.SEPARATOR);
        List<UserAccount> users = uManager.getUsers();
        int i = 0;
        for (UserAccount user : users)
        {
            i++;
            output.println("%d. %s".formatted(i, user.getUsername()));
        }
        Boolean inMenu = true;
        UserAccount selectedUser = null;
        while (inMenu)
        {
            output.println(this.SEPARATOR);
            output.print("Select username (Bank to exit): ");
            String username = readLine();
            if (username.isBlank()) return;
            selectedUser = uManager.getUser(username);
            // If cannot find the user
            if (selectedUser == null)
            {
                output.println("User not found.");
            }
            // If user tries to promote/demote themselves
            else if (selectedUser.getUsername().equals(currentUser.getUsername()))
            {
                output.println("User cannot be yourself.");
            }
            else
            {
                // Username is valid, we can go on
                inMenu = false;
            }
        }
        output.println(this.SEPARATOR);
        output.println("0. Exit\n1. Promote to Admin\n2. Demote to Standard User");
        inMenu = true;
        int option = 0;
        while (inMenu)
        {
            try
            {
                output.print("Select option: ");
                option = Integer.parseInt(readLine());
                if (option != 0 && option != 1 && option != 2) throw new NumberFormatException();
                // If everything works, we can leave
                inMenu = false;

            } catch (NumberFormatException e)
            {
                output.println("Invalid selection. Try again");
            }
        }
        output.println(this.SEPARATOR);
        if (option == 0) return; // Exit code means to leave
        if (option == 1) // Grant admin 
        {
            if (!selectedUser.isAdmin())
            {
                output.println("Promoted %s to admin".formatted(selectedUser.getUsername()));
                selectedUser.setAdmin(true);
                uManager.setUserAdmin(selectedUser.getUsername(), true);
            }
            else
            {
                output.println("User %s is already an admin".formatted(selectedUser.getUsername()));
                return;
            }
        }
        else if (option == 2) // Revoke admin
        {
            if (!selectedUser.isAdmin())
            {
                output.println("User %s is already not an admin".formatted(selectedUser.getUsername()));
                return;
            }
            else
            {
                output.println("Demoted %s from admin".formatted(selectedUser.getUsername()));
                selectedUser.setAdmin(false);
                uManager.setUserAdmin(selectedUser.getUsername(), false);
            }
        }
        // Log the event
        String contentType = selectedUser.isAdmin() ? "PROMOTION" : "DEMOTION";
        auditLog.log(currentUser.getUsername(), "ACCOUNT", contentType, selectedUser.getUsername());
    }

    public String[] userSelectionPrompt()
    {
        output.println(this.SEPARATOR);
        output.println("Login Menu");
        output.print("Username: ");
        String username = readLine();
        // Return null if no username to signal ending the program
        if (username.isBlank()) return null;
        output.print("Password: ");
        String password = readLine();
        output.println(this.SEPARATOR);

        return new String[] { username, password  };
    }

    public void message(String message)
    {
        output.println(message);
    }

    private void runTypeMenu(RecordType type) {
        while (true) {
            displayTypeMenu(type);
            switch (readLine()) {
                case "1" -> listRecords(type);
                case "2" -> readRecord(type);
                case "3" -> { if (isAdmin()) addRecord(type); else invalidSelection(); }
                case "4" -> { if (isAdmin() && type != RecordType.MISSION) importRecords(type); else invalidSelection(); }
                case "0" -> { return; }
                default -> invalidSelection();
            }
        }
    }

    // List, read and audit-log text comes from AppController, which the web view also uses.
    private void listRecords(RecordType type) {
        output.println(this.SEPARATOR);
        output.println(controller.listRecords(type));
        output.println(this.SEPARATOR);
    }

    private void readRecord(RecordType type) {
        output.println(this.SEPARATOR);
        Integer id = readId("Enter " + type.getSingular() + " ID: ");
        if (id == null) {
            return;
        }
        output.println(controller.readRecord(type, id, currentUser));
        output.println(this.SEPARATOR);
    }

    private void addRecord(RecordType type) {
        output.println(this.SEPARATOR);
        try {
            Integer id = switch (type) {
                case BRIEF -> bManager.addRecord(prompt("Title: "), prompt("Date (yyyy-mm-dd): "),
                        prompt("Text: ")).getId();
                case AGENT -> addAgent();
                case FACILITY -> fManager.addRecord(prompt("Name: "), prompt("Abbreviation: ")).getId();
                case MISSION -> addMission();
            };
            if (id != null) {
                auditLog.log(currentUser.getUsername(), "CREATE", type.name(), String.valueOf(id));
                output.println("Added " + type.getSingular() + " with ID " + id + ".");
            }
        } catch (IllegalArgumentException e) {
            output.println("Unable to add " + type.getSingular() + ": " + e.getMessage());
        }
        output.println(this.SEPARATOR);
    }

    private Boolean importRecords(RecordType type)
    {
        // TODO: Log imports. Rewrite aManager to allow for dynamic parameters (only first two are
        // guarenteed not null)
        output.println(this.SEPARATOR);
        output.print("Input path to file: ");
        String pathToTSV = readLine();
        int recordsAdded = 0;
        try {
            recordsAdded = switch (type) {
                case BRIEF -> bManager.importTSV(pathToTSV, bManager.getTableName(), "data", "mission_database.db");
                case AGENT -> aManager.importTSV(pathToTSV, aManager.getTableName(), "data", "mission_database.db");
                case FACILITY -> fManager.importTSV(pathToTSV, fManager.getTableName(), "data", "mission_database.db");
                case MISSION -> 0;
            };
        } catch (IllegalArgumentException e) {
            output.println("Unable to import TSV file.");
        } catch (IOException e) {
            output.println("Unable to find " + pathToTSV);
        } catch (Exception e) {
            output.println("Something went wrong.");
        }

        if (type == RecordType.MISSION)
        {
            output.println("Cannot import a mission file.");
            return false;
        }

        if (recordsAdded == 0) { output.println("Unable to import TSV file."); }
        else { output.println("TSV Successfully imported!"); }

        auditLog.log(currentUser.getUsername(), "IMPORT", type.getTitle(), "%d".formatted(recordsAdded));

        return recordsAdded > 0;
    }


    private Integer addAgent() {
        String name = prompt("Name: ");
        String born = prompt("Date of birth (yyyy-mm-dd): ");
        String died = blankToNull(prompt("Date of death (yyyy-mm-dd, blank if none): "));
        String notes = blankToNull(prompt("Notes (blank if none): "));
        Integer facilityId = readId("Facility ID: ");
        return facilityId == null ? null : aManager.addRecord(name, born, died, notes, facilityId).getId();
    }

    private Integer addMission() {
        String title = prompt("Title: ");
        // Get list of Briefs
        String[] briefNames = prompt("Brief titles, comma separated: ").trim().split(",");
        List<Brief> briefRecs = new ArrayList<>();
        for (int i = 0; i < briefNames.length; i++)
        {
            briefRecs.addAll( bManager.getRecords(bManager.getColumnNames()[1], briefNames[i]) ); 
        }

        // Get list of Agents
        String[] agentNames = prompt("Agent names, comma separated: ").trim().split(",");
        List<Agent> agentRecs = new ArrayList<>();
        for (int i = 0; i < agentNames.length; i++)
        {
            agentRecs.addAll( aManager.getRecords(aManager.getColumnNames()[1], agentNames[i]) );
        }

        String facilityName = prompt("Facility name: ");
        Facility facility = fManager.getRecords(fManager.getColumnNames()[1], facilityName).get(0);
    }

    private void displayAuditLog() {
        output.println(this.SEPARATOR);
        output.println(controller.auditLog(currentUser));
        output.println(this.SEPARATOR);
    }

    private boolean isAdmin() {
        return currentUser.isAdmin();
    }

    private String readLine() {
        return scanner.nextLine().trim();
    }

    private String prompt(String label) {
        output.print(label);
        return readLine();
    }

    private Integer readId(String label) {
        try {
            return Integer.parseInt(prompt(label));
        } catch (NumberFormatException e) {
            output.println("Invalid ID.");
            return null;
        }
    }

    private void invalidSelection() {
        output.println("Invalid selection. Please try again.");
    }

    private static String blankToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
