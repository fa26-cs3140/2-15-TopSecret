package org.view;

import org.controller.AppController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.managers.*;
import org.model.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class AgencyInterfaceMissionManagerTest {
    @TempDir Path directory;
    private String url;
    private BriefManager briefs;
    private AgentManager agents;
    private FacilityManager facilities;
    private MissionManager missions;
    private AuditLogManager audit;
    private UserAccountManager accounts;

    @BeforeEach
    void setUp() {
        url = "jdbc:sqlite:" + directory.resolve("db.sqlite");
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
        accounts.createFirstUser("admin", "Secret1");
        audit = new AuditLogManager(url);
        facilities.addRecord("HQ", "HQ", 1);
    }

    @Test
    void quotedTerminalSelectionsPreserveBothMissionAssignments() {
        int brief = briefs.addRecord("O'Brien", "2020-01-01", "Notes").getId();
        int agent = agents.addRecord("O'Neil", "1990-01-01", null, null, 1).getId();
        new AgencyInterface(agents, facilities, briefs, accounts, missions, audit, accounts.getUser("admin"),
                new Scanner("4\n3\nRecon\nO'Brien\nO'Neil\nHQ\n0\n0\n"),
                new PrintStream(OutputStream.nullOutputStream())).run();
        var saved = missions.getRecords("id", "%").getFirst();
        assertArrayEquals(new int[]{brief}, saved.getBriefIds());
        assertArrayEquals(new int[]{agent}, saved.getAgentIds());
    }

    @Test
    void lookupInputIsLiteralAndColumnNamesAreWhitelisted() {
        briefs.addRecord("Normal", "2020-01-01", "Notes");
        agents.addRecord("Normal", "1990-01-01", null, null, 1);
        assertTrue(briefs.getRecords("title", "' OR 1=1 --").isEmpty());
        assertTrue(agents.getRecords("name", "' OR 1=1 --").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> briefs.getRecords("id OR 1=1", "%"));
        assertThrows(IllegalArgumentException.class, () -> agents.getRecords("id OR 1=1", "%"));
    }

    @Test
    void failedReadsDoNotBecomeEmptySearchesOrMissingRecords() throws Exception {
        try (var c = DriverManager.getConnection(url); var s = c.createStatement()) {
            s.execute("DROP TABLE briefs");
            s.execute("DROP TABLE agents");
        }
        assertThrows(IllegalStateException.class, () -> briefs.getRecords("id", "%"));
        assertThrows(IllegalStateException.class, () -> agents.getRecords("id", "%"));
        var search = new RecordSearch(briefs, agents, facilities, missions);
        assertThrows(IllegalStateException.class, () -> search.find("clue"));
    }
}
