package org.view;

import org.controller.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.managers.*;
import org.model.Cipher;
import org.model.CipherKey;
import org.model.UserAccount;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class TerminalRecordIntegrityIntegrationTest {

    @TempDir
    Path directory;

    private String url;
    private FacilityManager facilities;
    private BriefManager briefs;
    private AgentManager agents;
    private MissionManager missions;
    private UserAccountManager accounts;
    private AuditLogManager audit;
    private UserAccount loggedInAdmin;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        url = "jdbc:sqlite:" + directory.resolve("agency.db");
        facilities = new FacilityManager(url);
        briefs = new BriefManager(url);
        agents = new AgentManager(url);
        missions = new MissionManager(url);
        briefs.createTable();
        agents.createTable();
        missions.createTable();
        String key = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:";
        accounts = new UserAccountManager(url, new FileHandler(directory.toString()),
                new Cipher(new CipherKey(key + "\n" + key.substring(1) + key.charAt(0))));
        loggedInAdmin = accounts.createFirstUser("admin", "Secret1");
        audit = new AuditLogManager(url);
    }

    @Test
    void demotionBlocksCreationAndAuditReviewInAnExistingTerminalLogin() {
        AgencyInterface ui = ui("13\n3\n0\n6\n0\n");
        accounts.setUserAdmin("admin", false);

        ui.run();

        assertEquals(196, facilities.getCountries().size());
        assertTrue(output.toString().contains("Invalid selection"));
        assertFalse(output.toString().contains("6. Review audit log"));
        assertTrue(audit.getAuditLog().stream().noneMatch(entry -> entry.getAction().equals("CREATE")));
    }

    @Test
    void demotionDuringCreationIsCheckedAgainBeforeTheTransaction() {
        // Demote after the Add selection but before the country name is returned.
        Readable input = new Readable() {
            private final java.util.Iterator<String> lines = List.of("13\n", "3\n", "Denied\n", "0\n", "0\n").iterator();

            @Override
            public int read(java.nio.CharBuffer target) {
                if (!lines.hasNext()) {
                    return -1;
                }
                String line = lines.next();
                if (line.equals("Denied\n")) {
                    accounts.setUserAdmin("admin", false);
                }
                target.put(line);
                return line.length();
            }
        };
        AgencyInterface ui = new AgencyInterface(agents, facilities, briefs, accounts, missions, audit,
                loggedInAdmin, new Scanner(input), new PrintStream(output));

        assertDoesNotThrow(ui::run);

        assertNull(facilities.getCountry("Denied"));
        assertTrue(output.toString().contains("Only administrators"));
    }

    @Test
    void auditFailureRollsBackEveryTerminalContentTypeAndKeepsTheMenuUsable() throws Exception {
        int facility = facilities.addRecord("HQ", "HQ", 1).getId();
        briefs.addRecord("Existing brief", "2020-01-01", "Text");
        agents.addRecord("ghost", "1990-01-01", null, null, facility);
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_create BEFORE INSERT ON audit_log "
                    + "BEGIN SELECT RAISE(ABORT, 'test'); END");
        }
        List<String> inputs = List.of(
                "1\n3\nDenied brief\n2020-01-01\nText\n0\n0\n",
                "2\n3\nDenied agent\n1990-01-01\n\n\n" + facility + "\n0\n0\n",
                "3\n3\nDenied HQ\nDHQ\n1\n0\n0\n",
                "4\n3\nDenied mission\nExisting brief\nghost\nHQ\n0\n0\n",
                "13\n3\nDenied country\n0\n0\n");
        for (String input : inputs) {
            output.reset();
            assertDoesNotThrow(() -> ui(input).run());
            assertTrue(output.toString().contains("Unable to add"));
            assertTrue(output.toString().contains("Exiting Top Secret"));
        }
        assertEquals(1, briefs.getRecords("id", "%").size());
        assertEquals(1, agents.getRecords("id", "%").size());
        assertEquals(1, facilities.getRecords("id", "%").size());
        assertTrue(missions.getRecords("id", "%").isEmpty());
        assertEquals(196, facilities.getCountries().size());
        assertTrue(audit.getAuditLog().stream().noneMatch(entry -> entry.getAction().equals("CREATE")));
    }

    @Test
    void successfulTerminalCreatesPersistExactlyOneAuditEntryEach() {
        assertDoesNotThrow(() -> ui("13\n3\nTest islands\n0\n3\n3\nHQ\nHQ\n1\n0\n"
                + "1\n3\nNotes\n2020-01-01\nText\n0\n"
                + "2\n3\nghost\n1990-01-01\n\n\n1\n0\n"
                + "4\n3\nRecon\nNotes\nghost\nHQ\n0\n0\n").run());

        var creates = audit.getAuditLog().stream().filter(entry -> entry.getAction().equals("CREATE")).toList();
        assertEquals(List.of("COUNTRY", "FACILITY", "BRIEF", "AGENT", "MISSION"),
                creates.stream().map(org.model.AuditLogEntry::getContentType).toList());
        assertTrue(creates.stream().allMatch(entry -> entry.getUsername().equals("admin")
                && Integer.parseInt(entry.getContentIdentifier()) > 0));
        assertEquals(1, missions.getRecords("id", "%").size());
        assertArrayEquals(new int[]{1}, missions.getRecords("id", "%").getFirst().getBriefIds());
        assertArrayEquals(new int[]{1}, missions.getRecords("id", "%").getFirst().getAgentIds());
    }

    private AgencyInterface ui(String input) {
        AgencyInterface ui = new AgencyInterface(agents, facilities, briefs, accounts, missions, audit,
                loggedInAdmin, new Scanner(input), new PrintStream(output));
        ui.enableSessionManagement(new SessionManager(accounts));
        return ui;
    }
}
