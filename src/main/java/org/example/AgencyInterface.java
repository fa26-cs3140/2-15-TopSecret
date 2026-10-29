package org.example;

import java.io.PrintStream;
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
    private final AuditLogService auditLog;
    private final UserAccount currentUser;
    private final Scanner scanner;
    private final PrintStream output;

    public AgencyInterface(AgentManager aManager, FacilityManager fManager, BriefManager bManager,
                           AuditLogService auditLog, UserAccount currentUser, Scanner scanner, PrintStream output) {
        if (aManager == null || fManager == null || bManager == null || auditLog == null || currentUser == null || scanner == null || output == null) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        // this.data = data;
        this.aManager = aManager;
        this.fManager = fManager;
        this.bManager = bManager;
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
            case BRIEF -> bManager.getRecords("id", "%").stream().map(b -> b.getId() + ". " + b.getTitle()).toList();
            case AGENT -> aManager.getRecords("id", "%").stream().map(a -> a.getId() + ". " + a.getName()).toList();
            case FACILITY -> fManager.getRecords("id", "%").stream()
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
    }

    private void addRecord(RecordType type) {
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
