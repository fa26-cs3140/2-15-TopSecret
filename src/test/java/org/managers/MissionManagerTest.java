package org.managers;

import org.model.Mission;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MissionManagerTest {
    @TempDir Path tempDir;
    private String url;
    private MissionManager manager;
    @BeforeEach void setup() {
        url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        var facilities = new FacilityManager(url); facilities.createTable();
        facilities.addRecord("HQ", "HQ");
        manager = new MissionManager(url); manager.createTable();
    }
    @Test void constructorAndMetadata() {
        for (String invalid : new String[]{null, "", " "})
            assertThrows(IllegalArgumentException.class, () -> new MissionManager(invalid));
        assertEquals(url, manager.getURL()); assertEquals("missions", manager.getTableName());
        assertArrayEquals(new String[]{"id","title","briefIds","agentIds","facilityId"}, manager.getColumnNames());
        String[] copy = manager.getColumnNames(); copy[0] = "oops";
        assertEquals("id", manager.getColumnNames()[0]);
    }
    @Test void schemaIsCreatedAndIdempotent() throws Exception {
        manager.createTable();
        try (var c = DriverManager.getConnection(url); var s = c.createStatement();
             var rs = s.executeQuery("PRAGMA table_info(missions)")) {
            var names = new java.util.ArrayList<String>(); var types = new java.util.ArrayList<String>();
            while (rs.next()) { names.add(rs.getString("name")); types.add(rs.getString("type")); }
            assertEquals(List.of("id","title","briefIds","agentIds","facilityId"), names);
            assertEquals(List.of("INTEGER","TEXT","TEXT","TEXT","INTEGER"), types);
        }
    }
    @Test void nullListsAreEmptyAndConstructionPreservesRecords() {
        manager.addRecord("Empty mission", null, null, 1);
        var reopened = new MissionManager(url); reopened.createTable();
        var rows = reopened.getRecords("id", "%"); assertEquals(1, rows.size());
        assertArrayEquals(new int[0], rows.get(0).getBriefIds());
        assertArrayEquals(new int[0], rows.get(0).getAgentIds());
    }
    @Test void distinctBriefAndAgentListsRoundTrip() {
        var saved = manager.addRecord(new Mission(1,"O'Brien mission",new int[]{2,3},new int[]{7,8},1));
        var loaded = manager.getRecords("id", String.valueOf(saved.getId())).get(0);
        assertEquals("O'Brien mission", loaded.getTitle()); assertEquals(1, loaded.getFacilityId());
        assertArrayEquals(new int[]{2,3}, loaded.getBriefIds()); assertArrayEquals(new int[]{7,8}, loaded.getAgentIds());
    }
    @Test void allRowsAreMappedIndependentlyAndNoMatchIsEmpty() {
        manager.addRecord("one",new int[]{2},new int[]{7},1);
        manager.addRecord("two",new int[]{3},new int[]{8},1);
        var rows = manager.getRecords("id","%"); assertEquals(2, rows.size());
        assertEquals("one",rows.get(0).getTitle()); assertEquals("two",rows.get(1).getTitle());
        assertArrayEquals(new int[]{8},rows.get(1).getAgentIds());
        assertTrue(manager.getRecords("title","missing").isEmpty());
        assertTrue(manager.getRecords("title","' OR 1=1 --").isEmpty());
    }
    @Test void invalidRecordsAndFacilityReferencesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->manager.addRecord((Mission)null));
        assertThrows(IllegalArgumentException.class,()->manager.addRecord(" ",null,null,1));
        assertThrows(IllegalArgumentException.class,()->manager.addRecord("invalid",new int[]{0},null,1));
        assertThrows(IllegalArgumentException.class,()->manager.addRecord("invalid",null,null,99));
        assertTrue(manager.getRecords("id","%").isEmpty());
    }
    @Test void updatesAreBoundAndColumnsValidated() {
        manager.addRecord("one",null,null,1);
        assertTrue(manager.updateRecord("title","one","title","O'Brien"));
        assertTrue(manager.updateRecord("title","O'Brien","agentIds","7, 8"));
        assertArrayEquals(new int[]{7,8},manager.getRecords("title","O'Brien").get(0).getAgentIds());
        assertFalse(manager.updateRecord("id","99","title","not changed"));
        assertThrows(IllegalArgumentException.class,()->manager.getRecords("id; DROP TABLE missions","%"));
        assertThrows(IllegalArgumentException.class,()->manager.updateRecord("id","1","invalid","x"));
        assertThrows(IllegalArgumentException.class,()->manager.updateRecord("id","1","title"," "));
        assertThrows(IllegalArgumentException.class,()->manager.updateRecord("id","1","agentIds","abc"));
        assertThrows(IllegalArgumentException.class,()->manager.updateRecord("id","1","facilityId","0"));
    }
    @Test void importsAllFourFieldsAndValidatesBeforeSaving() throws Exception {
        var path = tempDir.resolve("missions.tsv");
        Files.writeString(path,"title\tbriefIds\tagentIds\tfacilityId\none\t2,3\t7,8\t1\ntwo\t\t\t1\n");
        assertEquals(2,manager.importTSV(path.toString(),"missions","unused","unused"));
        assertArrayEquals(new int[]{7,8},manager.getRecords("title","one").get(0).getAgentIds());
        Files.writeString(path,"header\nvalid\t\t\t1\ninvalid\tbad\t\t1\n");
        assertThrows(IllegalArgumentException.class,()->manager.importTSV(path.toString(),"missions","unused","unused"));
        assertEquals(2,manager.getRecords("id","%").size());
        assertThrows(IllegalArgumentException.class,()->manager.importTSV(path.toString(),"other","unused","unused"));
        assertThrows(java.io.IOException.class,()->manager.importTSV("missing-file.tsv","missions","unused","unused"));
    }
}
