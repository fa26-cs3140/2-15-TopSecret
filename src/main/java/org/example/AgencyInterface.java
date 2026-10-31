package org.example;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.managers.AgentManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;

public class AgencyInterface {

    // private final AgencyDataService data;
    private final AgentManager aManager;
    private final FacilityManager fManager;
    private final BriefManager bManager;
    private final UserAccountManager uManager;
    private final AuditLogService auditLog;
    private UserAccount currentUser;
    private final Scanner scanner;
    private final PrintStream output;

    private final String SEPARATOR= "-----------------";

    public AgencyInterface(AgentManager aManager, FacilityManager fManager, BriefManager bManager, UserAccountManager uManager, 
                           AuditLogService auditLog, UserAccount currentUser, Scanner scanner, PrintStream output) {
        if (aManager == null || fManager == null || bManager == null || auditLog == null || currentUser == null || scanner == null || output == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        // this.data = data;
        this.aManager = aManager;
        this.fManager = fManager;
        this.bManager = bManager;
        this.uManager = uManager;
        this.auditLog = auditLog;
        this.currentUser = currentUser;
        this.scanner = scanner;
        this.output = output;
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
                    case "4" -> userCreationMenu();
                    case "5" -> { if (isAdmin()) displayAuditLog(); else invalidSelection(); }
                    case "6" -> { if (isAdmin()) userPromotionMenu(); else invalidSelection(); } // TODO: Promote/Demote admin *cannot demote self
                    case "0" -> { output.println("Exiting Top Secret."); return; }
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
        output.print("0. Exit\nSelect an option: ");
    }

    public void displayTypeMenu(RecordType type) {
        output.println("\n" + type.getTitle() + "\n1. List\n2. Read");
        if (isAdmin()) {
            output.println("3. Add");
            output.println("4. Import");
        }
        output.print("0. Back\nSelect an option: ");
    }

    public String[] userCreationPrompt()
    {
        output.println(this.SEPARATOR);
        output.println("User Creation Menu");
        output.print("Username: ");
        String username = readLine();
        output.print("Password: ");
        String password = readLine();
        output.println(this.SEPARATOR);
        
        return new String[] { username, password };

    }

    private void userCreationMenu()
    {
        output.println(this.SEPARATOR);
        output.println("Create new user account");
        String[] data = this.userCreationPrompt();
        uManager.createUser(data[0], false);
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
            output.print("Select username: ");
            String username = readLine();
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
        output.println("1. Promote to Admin\n2. Demote to Standard User");
        inMenu = true;
        int option = 0;
        while (inMenu)
        {
            try
            {
                output.print("Select option: ");
                option = Integer.parseInt(readLine());
                if (option != 1 && option != 2) throw new NumberFormatException();
                // If everything works, we can leave
                inMenu = false;

            } catch (NumberFormatException e)
            {
                output.println("Invalid selection. Try again");
            }
        }
        output.println(this.SEPARATOR);
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
                case "4" -> { if (isAdmin()) importRecords(type); else invalidSelection(); }
                case "0" -> { return; }
                default -> invalidSelection();
            }
        }
    }

    private void listRecords(RecordType type) {
        output.println(this.SEPARATOR);
        List<String> lines = switch (type) {
            case BRIEF -> bManager.getRecords("id", "%").stream().map(b -> b.getId() + ". " + b.getTitle()).toList();
            case AGENT -> aManager.getRecords("id", "%").stream().map(a -> a.getId() + ". " + a.getName()).toList();
            case FACILITY -> fManager.getRecords("id", "%").stream()
                    .map(f -> f.getId() + ". " + f.getName() + " (" + f.getAbbreviation() + ")").toList();
        };
        if (lines.isEmpty()) {
            output.println("No " + type.getTitle().toLowerCase() + " are available.");
        }
        lines.forEach(output::println);
        output.println(this.SEPARATOR);
    }

    private void readRecord(RecordType type) {
        output.println(this.SEPARATOR);
        Integer id = readId("Enter " + type.getSingular() + " ID: ");
        if (id == null) {
            return;
        }
        List<String> details = switch (type) {
            case BRIEF -> bManager.getRecords("id", "%").stream().filter(b -> b.getId() == id).findFirst()
                    .map(b -> List.of("Title: " + b.getTitle(), "Date: " + b.getDate(), "Text: " + b.getText()))
                    .orElse(null);
            case AGENT -> aManager.getRecords("id", "%").stream().filter(a -> a.getId() == id).findFirst()
                    .map(a -> List.of("Name: " + a.getName(), "Date of birth: " + a.getDateOfBirth(),
                            "Date of death: " + orNA(a.getDateOfDeath()), "Notes: " + orNA(a.getNotes()),
                            "Facility ID: " + a.getFacilityId()))
                    .orElse(null);
            case FACILITY -> fManager.getRecords("id", "%").stream().filter(f -> f.getId() == id).findFirst()
                    .map(f -> List.of("Name: " + f.getName(), "Abbreviation: " + f.getAbbreviation()))
                    .orElse(null);
        };
        if (details == null) {
            output.println("No " + type.getSingular() + " found with ID " + id + ".");
            return;
        }
        details.forEach(output::println);
        auditLog.log(currentUser.getUsername(), "READ", type.name(), String.valueOf(id));
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
            };
        } catch (IllegalArgumentException e) {
            output.println("Unable to import TSV file.");
        } catch (IOException e) {
            output.println("Unable to find " + pathToTSV);
        } catch (Exception e) {
            output.println("Something went wrong.");
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

    private void displayAuditLog() {
        output.println(this.SEPARATOR);
        List<AuditLogEntry> entries = auditLog.getAuditLog();
        if (entries == null || entries.isEmpty()) {
            output.println("No audit entries have been recorded.");
            return;
        }
        for (AuditLogEntry e : entries) {
            output.println(e.getTimestamp() + " | " + e.getUsername() + " | " + e.getAction()
                    + " | " + e.getContentType() + " " + e.getContentIdentifier());
        }
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

    private static String orNA(String value) {
        return value == null ? "N/A" : value;
    }
}
