package org.example;

import java.io.PrintStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Predicate;

public class UserInterface {

    private final AgencyDataService data;
    private final AuditLogService auditLog;
    private final UserAccount currentUser;
    private final Scanner scanner;
    private final PrintStream output;

    public UserInterface(AgencyDataService data, AuditLogService auditLog,
                         UserAccount currentUser, Scanner scanner, PrintStream output) {
        if (data == null || auditLog == null || currentUser == null || scanner == null || output == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        this.data = data;
        this.auditLog = auditLog;
        this.currentUser = currentUser;
        this.scanner = scanner;
        this.output = output;
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }

    // Returns the logged-in user, or null after 3 failed attempts or if input runs out.
    public static UserAccount login(UserAccountManager accounts, Scanner scanner, PrintStream output) {
        try {
            if (!accounts.hasUsers()) {
                output.println("No accounts exist. Create the first (admin) account.");
                String username = promptUntilValid(scanner, output, "Username: ", accounts::isValidUsername,
                        "Usernames need 3+ letters, numbers, hyphens, or underscores.");
                String password = promptNewPassword(accounts, scanner, output);
                return accounts.createFirstUser(username, password);
            }

            for (int attempt = 0; attempt < 3; attempt++) {
                output.print("Username: ");
                String username = scanner.nextLine().trim();
                UserAccount user = accounts.getUser(username);

                if (user != null && !accounts.passwordExists(username)) {
                    output.println("First login: create your password.");
                    accounts.setInitialPassword(username, promptNewPassword(accounts, scanner, output));
                    return user;
                }

                output.print("Password: ");
                String password = scanner.nextLine();
                if (user != null && accounts.verifyLogin(username, password)) {
                    return user;
                }
                output.println("Incorrect username or password.");
            }
            output.println("Too many failed attempts.");
        } catch (NoSuchElementException e) {
            output.println();
        }
        return null;
    }

    private static String promptNewPassword(UserAccountManager accounts, Scanner scanner, PrintStream output) {
        return promptUntilValid(scanner, output, "New password: ", accounts::isValidPassword,
                "Passwords need 5+ characters with uppercase and lowercase letters.");
    }

    private static String promptUntilValid(Scanner scanner, PrintStream output, String label,
                                           Predicate<String> isValid, String rules) {
        while (true) {
            output.print(label);
            String value = scanner.nextLine().trim();
            if (isValid.test(value)) {
                return value;
            }
            output.println(rules);
        }
    }

    public void run() {
        try {
            while (true) {
                displayMainMenu();
                switch (readLine()) {
                    case "1" -> runTypeMenu(RecordType.BRIEF);
                    case "2" -> runTypeMenu(RecordType.AGENT);
                    case "3" -> runTypeMenu(RecordType.FACILITY);
                    case "4" -> { if (isAdmin()) displayAuditLog(); else invalidSelection(); }
                    case "0" -> { output.println("Exiting Top Secret."); return; }
                    default -> invalidSelection();
                }
            }
        } catch (NoSuchElementException e) {
            // Input ran out; stop quietly.
        }
    }

    public void displayMainMenu() {
        output.println("\nTop Secret Main Menu\n1. Briefs\n2. Agents\n3. Facilities");
        if (isAdmin()) {
            output.println("4. Review audit log");
        }
        output.print("0. Exit\nSelect an option: ");
    }

    public void displayTypeMenu(RecordType type) {
        output.println("\n" + type.getTitle() + "\n1. List\n2. Read");
        if (isAdmin()) {
            output.println("3. Add");
        }
        output.print("0. Back\nSelect an option: ");
    }

    private void runTypeMenu(RecordType type) {
        while (true) {
            displayTypeMenu(type);
            switch (readLine()) {
                case "1" -> listRecords(type);
                case "2" -> readRecord(type);
                case "3" -> { if (isAdmin()) addRecord(type); else invalidSelection(); }
                case "0" -> { return; }
                default -> invalidSelection();
            }
        }
    }

    private void listRecords(RecordType type) {
        List<String> lines = switch (type) {
            case BRIEF -> data.getBriefs().stream().map(b -> b.getId() + ". " + b.getTitle()).toList();
            case AGENT -> data.getAgents().stream().map(a -> a.getId() + ". " + a.getName()).toList();
            case FACILITY -> data.getFacilities().stream()
                    .map(f -> f.getId() + ". " + f.getName() + " (" + f.getAbbreviation() + ")").toList();
        };
        if (lines.isEmpty()) {
            output.println("No " + type.getTitle().toLowerCase() + " are available.");
        }
        lines.forEach(output::println);
    }

    private void readRecord(RecordType type) {
        Integer id = readId("Enter " + type.getSingular() + " ID: ");
        if (id == null) {
            return;
        }
        List<String> details = switch (type) {
            case BRIEF -> data.getBriefs().stream().filter(b -> b.getId() == id).findFirst()
                    .map(b -> List.of("Title: " + b.getTitle(), "Date: " + b.getDate(), "Text: " + b.getText()))
                    .orElse(null);
            case AGENT -> data.getAgents().stream().filter(a -> a.getId() == id).findFirst()
                    .map(a -> List.of("Name: " + a.getName(), "Date of birth: " + a.getDateOfBirth(),
                            "Date of death: " + orNA(a.getDateOfDeath()), "Notes: " + orNA(a.getNotes()),
                            "Facility ID: " + a.getFacilityId()))
                    .orElse(null);
            case FACILITY -> data.getFacilities().stream().filter(f -> f.getId() == id).findFirst()
                    .map(f -> List.of("Name: " + f.getName(), "Abbreviation: " + f.getAbbreviation()))
                    .orElse(null);
        };
        if (details == null) {
            output.println("No " + type.getSingular() + " found with ID " + id + ".");
            return;
        }
        details.forEach(output::println);
        auditLog.log(currentUser.getUsername(), "READ", type.name(), String.valueOf(id));
    }

    private void addRecord(RecordType type) {
        try {
            Integer id = switch (type) {
                case BRIEF -> data.addBrief(prompt("Title: "), prompt("Date (yyyy-mm-dd): "),
                        prompt("Text: ")).getId();
                case AGENT -> addAgent();
                case FACILITY -> data.addFacility(prompt("Name: "), prompt("Abbreviation: ")).getId();
            };
            if (id != null) {
                auditLog.log(currentUser.getUsername(), "CREATE", type.name(), String.valueOf(id));
                output.println("Added " + type.getSingular() + " with ID " + id + ".");
            }
        } catch (IllegalArgumentException e) {
            output.println("Unable to add " + type.getSingular() + ": " + e.getMessage());
        }
    }

    private Integer addAgent() {
        String name = prompt("Name: ");
        String born = prompt("Date of birth (yyyy-mm-dd): ");
        String died = blankToNull(prompt("Date of death (yyyy-mm-dd, blank if none): "));
        String notes = blankToNull(prompt("Notes (blank if none): "));
        Integer facilityId = readId("Facility ID: ");
        return facilityId == null ? null : data.addAgent(name, born, died, notes, facilityId).getId();
    }

    private void displayAuditLog() {
        List<AuditLogEntry> entries = auditLog.getAuditLog();
        if (entries == null || entries.isEmpty()) {
            output.println("No audit entries have been recorded.");
            return;
        }
        for (AuditLogEntry e : entries) {
            output.println(e.getTimestamp() + " | " + e.getUsername() + " | " + e.getAction()
                    + " | " + e.getContentType() + " " + e.getContentIdentifier());
        }
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
