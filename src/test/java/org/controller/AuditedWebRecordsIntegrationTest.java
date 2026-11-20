package org.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.managers.AgentManager;
import org.managers.AuditLogManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.FileHandler;
import org.managers.MissionManager;
import org.managers.SqliteAuditLogStore;
import org.managers.UserAccountManager;
import org.model.Cipher;
import org.model.CipherKey;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AuditedWebRecordsIntegrationTest {

    @TempDir
    Path directory;

    private final HttpClient client = HttpClient.newHttpClient();
    private String url;
    private WebServer server;
    private UserAccountManager accounts;
    private AuditLogManager audit;
    private String cookie;
    private String standardCookie;

    @BeforeEach
    void setUp() throws Exception {
        url = "jdbc:sqlite:" + directory.resolve("agency.db");
        FacilityManager facilities = new FacilityManager(url);
        BriefManager briefs = new BriefManager(url);
        AgentManager agents = new AgentManager(url);
        MissionManager missions = new MissionManager(url);
        facilities.createTable();
        briefs.createTable();
        agents.createTable();
        missions.createTable();
        String original = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:";
        String ciphered = original.substring(1) + original.charAt(0);
        accounts = new UserAccountManager(url, new FileHandler(directory.toString()),
                new Cipher(new CipherKey(original + "\n" + ciphered)));
        accounts.createFirstUser("admin", "Secret1");
        accounts.createUser("ghost", false);
        assertTrue(accounts.setInitialPassword("ghost", "OtherPass2"));
        audit = new AuditLogManager(url);
        server = new WebServer(new AppController(briefs, agents, missions, facilities, audit),
                accounts, new SessionManager(accounts), 0);
        server.start();
        cookie = login("admin", "Secret1");
        standardCookie = login("ghost", "OtherPass2");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void createsEditsAndNavigatesAllContentTypesWithPersistedAudit() throws Exception {
        int facility = create("facilities", Map.of("name", "HQ <West>", "abbreviation", "HQ", "countryId", "1"));
        int brief = create("briefs", Map.of("title", "O'Brien", "date", "2020-01-01", "text", "  Line one\nLine two\n"));
        int agent = create("agents", Map.of("name", "Ghost", "dateOfBirth", "1990-01-01", "facilityId", "" + facility));
        int mission = create("missions", Map.of("title", "Recon", "facilityId", "" + facility,
                "briefIds", "" + brief, "agentIds", "" + agent));

        assertLink(get("/facilities/" + facility, standardCookie), "/agents/" + agent);
        assertLink(get("/facilities/" + facility, cookie), "/missions/" + mission);
        assertLink(get("/missions/" + mission, cookie), "/briefs/" + brief);
        assertLink(get("/agents/" + agent, cookie), "/missions/" + mission);
        assertLink(get("/briefs/" + brief, cookie), "/missions/" + mission);
        assertTrue(get("/facilities/" + facility, cookie).body().contains("HQ &lt;West&gt;"));

        edit("facilities", facility, Map.of("name", "New HQ", "abbreviation", "NH", "countryId", "1"));
        edit("briefs", brief, Map.of("title", "Changed", "date", "2021-02-03", "text", "  O'Brien\n100%\n"));
        edit("agents", agent, Map.of("name", "Ghost", "dateOfBirth", "1990-01-01",
                "facilityId", "" + facility, "notes", "Field update"));
        edit("missions", mission, Map.of("title", "Recon 2", "facilityId", "" + facility,
                "briefIds", "", "agentIds", ""));

        assertFalse(get("/missions/" + mission, cookie).body().contains("href=\"/briefs/" + brief + "\""));
        assertEquals("  O'Brien\n100%\n", new BriefManager(url).getRecords("id", "" + brief).getFirst().getText());
        var writes = new SqliteAuditLogStore(url).getAll().stream()
                .filter(entry -> entry.getAction().equals("CREATE") || entry.getAction().equals("UPDATE")).toList();
        assertEquals(8, writes.size());
        for (var entry : writes) {
            assertEquals("admin", entry.getUsername());
            assertNotNull(entry.getTimestamp());
            assertTrue(Integer.parseInt(entry.getContentIdentifier()) > 0);
        }
    }

    @Test
    void countryRenamesAndReassignmentAppearAtFacilityLinks() throws Exception {
        int country = create("countries", Map.of("name", "O'Brien <Islands>"));
        assertTrue(get("/countries", cookie).body().contains("O&#39;Brien &lt;Islands&gt;"));
        int facility = create("facilities", Map.of("name", "Island HQ", "abbreviation", "IHQ",
                "countryId", String.valueOf(country)));
        assertLink(get("/facilities/" + facility, standardCookie), "/countries/" + country);
        assertLink(get("/countries/" + country, standardCookie), "/facilities/" + facility);

        edit("countries", country, Map.of("name", "Updated <Islands>"));
        assertTrue(get("/facilities/" + facility, cookie).body().contains("Updated &lt;Islands&gt;"));
        var manager = new FacilityManager(url);
        var results = new org.managers.RecordSearch(new BriefManager(url), new AgentManager(url),
                manager, new MissionManager(url)).find("updated <islands>");
        assertTrue(results.stream().anyMatch(match -> match.type() == org.model.RecordType.COUNTRY
                && match.id() == country));
        manager.createTable();
        assertEquals("Updated <Islands>", manager.getCountry(country).getName());

        edit("facilities", facility, Map.of("name", "Island HQ", "abbreviation", "IHQ", "countryId", "1"));
        assertLink(get("/facilities/" + facility, cookie), "/countries/1");
        assertFalse(get("/countries/" + country, cookie).body().contains("href=\"/facilities/" + facility + "\""));
        assertEquals(403, post("/countries/" + country + "/edit", Map.of("name", "Denied"), standardCookie).statusCode());
        assertTrue(audit.getAuditLog().stream().anyMatch(entry ->
                entry.getContentType().equals("COUNTRY") && entry.getAction().equals("UPDATE")
                        && entry.getContentIdentifier().equals(String.valueOf(country))));
    }

    @Test
    void invalidCountryAndAuditFailureLeaveAssociationsAndNamesIntact() throws Exception {
        int country = create("countries", Map.of("name", "Original islands"));
        int facility = create("facilities", Map.of("name", "HQ", "abbreviation", "HQ",
                "countryId", String.valueOf(country)));
        HttpResponse<String> invalid = post("/facilities/" + facility + "/edit",
                Map.of("name", "Bad", "abbreviation", "BAD", "countryId", "999999"), cookie);
        assertTrue(invalid.body().contains("role=\"alert\""));
        assertEquals(country, new FacilityManager(url).getRecords("id", "" + facility).getFirst().getCountryID());

        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER reject_country_audit BEFORE INSERT ON audit_log "
                    + "BEGIN SELECT RAISE(ABORT, 'test'); END");
        }
        HttpResponse<String> failed = post("/countries/" + country + "/edit", Map.of("name", "Partial"), cookie);
        assertTrue(failed.body().contains("role=\"alert\""));
        assertEquals("Original islands", new FacilityManager(url).getCountry(country).getName());
    }

    @Test
    void rollbackAndAccessDenialAreVisibleThroughActualPostRoutes() throws Exception {
        int facility = create("facilities", Map.of("name", "Original", "abbreviation", "HQ", "countryId", "1"));
        long before = audit.getAuditLog().size();
        assertEquals(403, post("/facilities/" + facility + "/edit", Map.of("name", "No", "abbreviation", "NO", "countryId", "1"),
                standardCookie).statusCode());

        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("CREATE TRIGGER fail_audit BEFORE INSERT ON audit_log "
                    + "BEGIN SELECT RAISE(ABORT, 'test'); END");
        }
        HttpResponse<String> failed = post("/facilities/" + facility + "/edit",
                Map.of("name", "Partial", "abbreviation", "BAD", "countryId", "1"), cookie);
        assertTrue(failed.body().contains("role=\"alert\""));
        assertFalse(failed.headers().firstValue("Location").isPresent());
        assertEquals("Original", new FacilityManager(url).getRecords("id", "" + facility).getFirst().getName());
        assertEquals(before, audit.getAuditLog().size());
        assertEquals(404, get("/facilities/9999", cookie).statusCode());
    }

    @Test
    void demotedAdminLosesWebEditAccessOnNextRequest() throws Exception {
        accounts.setUserAdmin("admin", false);
        assertEquals(403, get("/briefs/new", cookie).statusCode());
        assertEquals(403, post("/briefs/new", Map.of("title", "Denied", "date", "2020-01-01", "text", "x"),
                cookie).statusCode());
        assertTrue(audit.getAuditLog().isEmpty());
    }

    @Test
    void passwordsRemainSeparateCipheredFilesAcrossEdits() throws Exception {
        assertTrue(accounts.verifyLogin("admin", "Secret1"));
        assertTrue(accounts.verifyLogin("ghost", "OtherPass2"));
        assertFalse(accounts.verifyLogin("ghost", "Secret1"));
        assertTrue(accounts.changePassword("ghost", "ChangedPass3"));
        assertFalse(accounts.verifyLogin("ghost", "OtherPass2"));
        assertTrue(accounts.verifyLogin("ghost", "ChangedPass3"));
        try (var files = java.nio.file.Files.list(directory)) {
            assertEquals(2, files.filter(path -> path.getFileName().toString().startsWith("credentials-")).count());
        }
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA table_info(users)")) {
            var columns = new java.util.ArrayList<String>();
            while (result.next()) {
                columns.add(result.getString("name"));
            }
            assertEquals(java.util.List.of("username", "is_admin"), columns);
        }
    }

    @Test
    void malformedFormEncodingReturnsAnErrorAndKeepsTheServerUsable() throws Exception {
        for (String path : new String[]{"/login", "/countries/new"}) {
            HttpRequest request = request(path, cookie)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("name=%ZZ&username=%"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(400, response.statusCode(), path);
        }
        assertEquals(200, get("/menu", cookie).statusCode());
        assertTrue(audit.getAuditLog().isEmpty());
    }

    @Test
    void slashOnlyPathsReturnNotFoundWithoutDroppingTheConnection() throws Exception {
        for (String path : new String[]{"//", "///", "////"}) {
            int status = get(path, cookie).statusCode();
            assertTrue(status == 400 || status == 404, path);
        }
        assertEquals(404, get("/%2F", cookie).statusCode());
        assertEquals(404, get("/%2F%2F", cookie).statusCode());
        assertEquals(200, get("/countries", cookie).statusCode());
    }

    @Test
    void aReadStorageFailureReturnsAControlledResponse() throws Exception {
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.execute("DROP TABLE countries");
        }
        HttpResponse<String> response = get("/countries", cookie);
        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("temporarily unavailable"));
        assertFalse(response.body().contains("no such table"));
        assertEquals(200, get("/menu", cookie).statusCode());
    }

    @Test
    void hostileNamesRemainLiteralAndCannotChangeOtherRecords() throws Exception {
        String hostile = "<script>alert(1)</script> O'Brien'); DROP TABLE countries; --";
        int country = create("countries", Map.of("name", hostile));
        int facility = create("facilities", Map.of("name", hostile, "abbreviation", "SAFE",
                "countryId", String.valueOf(country)));
        String detail = get("/facilities/" + facility, cookie).body();
        assertFalse(detail.contains("<script>"));
        assertTrue(detail.contains("&lt;script&gt;"));
        assertEquals(hostile, new FacilityManager(url).getCountry(country).getName());
        assertEquals(facility, new FacilityManager(url).getRecords("name", hostile).getFirst().getId());
        assertTrue(new FacilityManager(url).getCountries().size() > 1);
    }

    @Test
    void forgedRoleFieldsAndFakeTokensCannotAuthorizeAnEdit() throws Exception {
        Map<String, String> fields = Map.of("name", "Unauthorized", "username", "admin", "is_admin", "true");
        assertEquals(403, post("/countries/new", fields, standardCookie).statusCode());
        assertEquals(303, post("/countries/new", fields, "session=forged").statusCode());
        assertEquals(303, post("/countries/new", fields, null).statusCode());
        assertTrue(audit.getAuditLog().isEmpty());
    }

    @Test
    void deletingAnAccountInvalidatesItsExistingCookie() throws Exception {
        try (var connection = DriverManager.getConnection(url);
             var statement = connection.prepareStatement("DELETE FROM users WHERE username = ?")) {
            statement.setString(1, "ghost");
            statement.executeUpdate();
        }
        assertEquals(303, get("/countries", standardCookie).statusCode());
        assertEquals(303, post("/countries/new", Map.of("name", "Denied"), standardCookie).statusCode());
    }

    @Test
    void concurrentCountryCreatesKeepIdentifiersAndAuditEntriesConsistent() throws Exception {
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var store = new org.managers.SqliteRecordStore(url);
            var event = new org.managers.RecordEditor.AuditEvent("admin", "CREATE",
                    org.model.RecordType.COUNTRY, java.time.LocalDateTime.now());
            var futures = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
            for (int index = 0; index < 4; index++) {
                String name = "Concurrent country " + index;
                futures.add(workers.submit(() -> store.create(new org.model.Country(name, 1,
                        new org.model.Facility[0]), event)));
            }
            var ids = new java.util.HashSet<String>();
            for (var future : futures) {
                ids.add(String.valueOf(future.get(10, java.util.concurrent.TimeUnit.SECONDS)));
            }
            assertEquals(4, ids.size());
            var entries = audit.getAuditLog();
            assertEquals(4, entries.size());
            assertEquals(ids, entries.stream().map(org.model.AuditLogEntry::getContentIdentifier)
                    .collect(Collectors.toSet()));
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void failedAssignmentReadsReturn503WithoutOfferingAnEmptyEditForm() throws Exception {
        int facility = create("facilities", Map.of("name", "HQ", "abbreviation", "HQ", "countryId", "1"));
        int brief = create("briefs", Map.of("title", "Notes", "date", "2020-01-01", "text", "Text"));
        int agent = create("agents", Map.of("name", "ghost", "dateOfBirth", "1990-01-01",
                "facilityId", String.valueOf(facility)));
        int mission = create("missions", Map.of("title", "Recon", "facilityId", String.valueOf(facility),
                "briefIds", String.valueOf(brief), "agentIds", String.valueOf(agent)));
        for (String table : java.util.List.of("j_mission_briefs", "j_mission_agents", "missions")) {
            try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " RENAME TO unavailable_table");
            }
            try {
                var response = get("/missions/" + mission + "/edit", cookie);
                assertEquals(503, response.statusCode());
                assertFalse(response.body().contains("<form"));
                assertEquals(503, post("/missions/" + mission + "/edit", Map.of("title", "Changed",
                        "facilityId", String.valueOf(facility)), cookie).statusCode());
            } finally {
                try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE unavailable_table RENAME TO " + table);
                }
            }
        }
        var saved = new MissionManager(url).getRecords("id", String.valueOf(mission)).getFirst();
        assertArrayEquals(new int[]{brief}, saved.getBriefIds());
        assertArrayEquals(new int[]{agent}, saved.getAgentIds());
        assertEquals("Recon", saved.getTitle());
    }

    @Test
    void createUpdateAndReadAuditTimesUseUtcRegardlessOfLocalTimeZone() throws Exception {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        java.time.LocalDateTime before = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusSeconds(1);
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"));
            int country = create("countries", Map.of("name", "Clock test"));
            edit("countries", country, Map.of("name", "Updated clock test"));
            assertEquals(200, get("/countries/" + country, cookie).statusCode());
        } finally {
            java.util.TimeZone.setDefault(original);
        }
        java.time.LocalDateTime after = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(1);
        var entries = audit.getAuditLog();
        assertEquals(java.util.List.of("CREATE", "UPDATE", "READ"),
                entries.stream().map(org.model.AuditLogEntry::getAction).toList());
        assertTrue(entries.stream().allMatch(entry -> !entry.getTimestamp().isBefore(before)
                && !entry.getTimestamp().isAfter(after)));
    }

    @Test
    void crossOriginFormsAreRejectedEvenWithAValidAdministratorCookie() throws Exception {
        for (var headers : java.util.List.of(Map.of("Origin", "https://untrusted.example"),
                Map.of("Origin", "null"), Map.of("Sec-Fetch-Site", "cross-site"))) {
            var submission = request("/countries/new", cookie)
                    .header("Content-Type", "application/x-www-form-urlencoded");
            headers.forEach(submission::header);
            var response = client.send(submission.POST(HttpRequest.BodyPublishers.ofString("name=Denied"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(403, response.statusCode());
        }
        assertNull(new FacilityManager(url).getCountry("Denied"));
        assertTrue(audit.getAuditLog().isEmpty());

        var valid = client.send(request("/countries/new", cookie)
                .header("Origin", "http://localhost:" + server.getPort())
                .header("Sec-Fetch-Site", "same-origin")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("name=Allowed")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(303, valid.statusCode());
        assertNotNull(new FacilityManager(url).getCountry("Allowed"));
    }

    private String login(String username, String password) throws Exception {
        HttpResponse<String> response = post("/login", Map.of("username", username, "password", password), null);
        assertEquals(303, response.statusCode());
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
    }

    private int create(String segment, Map<String, String> values) throws Exception {
        HttpResponse<String> response = post("/" + segment + "/new", values, cookie);
        assertEquals(303, response.statusCode(), response.body());
        return Integer.parseInt(response.headers().firstValue("Location").orElseThrow().replace("/" + segment + "/", ""));
    }

    private void edit(String segment, int id, Map<String, String> values) throws Exception {
        HttpResponse<String> form = get("/" + segment + "/" + id + "/edit", cookie);
        assertEquals(200, form.statusCode());
        HttpResponse<String> response = post("/" + segment + "/" + id + "/edit", values, cookie);
        assertEquals(303, response.statusCode(), response.body());
    }

    private void assertLink(HttpResponse<String> response, String target) {
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("href=\"" + target + "\""), response.body());
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        return client.send(request(path, cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Map<String, String> values, String cookie) throws Exception {
        String form = values.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return client.send(request(path, cookie).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path, String cookie) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + path));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return request;
    }
}
