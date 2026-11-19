package org.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.managers.AgentManager;
import org.managers.AuditLogService;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.managers.UserAccountManager;
import org.model.Brief;
import org.model.Facility;
import org.model.UserAccount;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Web create/edit workflow through a real server on a test port (3002 stays free for the app).
class EditWorkflowIntegrationTest {

    private static final int TEST_PORT = 3014;

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private BriefManager briefs;
    private AuditLogService audit;
    private WebServer server;

    @BeforeEach
    void setUp() throws Exception {
        UserAccountManager accounts = mock(UserAccountManager.class);
        when(accounts.hasUsers()).thenReturn(true);
        when(accounts.verifyLogin("pat", "Secret1")).thenReturn(true);
        when(accounts.getUser("pat")).thenReturn(new UserAccount("pat", true));
        when(accounts.verifyLogin("agent7", "Agent77")).thenReturn(true);
        when(accounts.getUser("agent7")).thenReturn(new UserAccount("agent7", false));

        briefs = mock(BriefManager.class);
        FacilityManager facilities = mock(FacilityManager.class);
        when(facilities.getRecords("id", "%")).thenReturn(List.of(new Facility(1, "Blackwood Annex", "BA")));
        audit = mock(AuditLogService.class);

        AppController controller = new AppController(briefs, mock(AgentManager.class), mock(MissionManager.class),
                facilities, audit);
        server = new WebServer(controller, accounts, new SessionManager(), TEST_PORT);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private HttpResponse<String> get(String path, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + TEST_PORT + path));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String form, String cookie) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + TEST_PORT + path))
                .header("Content-Type", "application/x-www-form-urlencoded");
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(form)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String login(String username, String password) throws Exception {
        HttpResponse<String> response = post("/login", "username=" + username + "&password=" + password, null);
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
    }

    @Test
    void editPagesRequireLogin() throws Exception {
        for (String path : new String[]{"/briefs/new", "/facilities/1/edit"}) {
            HttpResponse<String> response = get(path, null);

            assertEquals(303, response.statusCode(), path);
            assertEquals("/", response.headers().firstValue("Location").orElseThrow(), path);
        }
        assertEquals(303, post("/briefs/new", "title=x", null).statusCode());
        verifyNoInteractions(briefs);
    }

    @Test
    void adminSeesEmptyNewForm() throws Exception {
        HttpResponse<String> response = get("/briefs/new", login("pat", "Secret1"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("action=\"/briefs/new\""));
        assertTrue(response.body().contains("name=\"title\""));
        assertTrue(response.body().contains("name=\"date\""));
    }

    @Test
    void standardUserIsDeniedEditing() throws Exception {
        String cookie = login("agent7", "Agent77");

        assertEquals(403, get("/briefs/new", cookie).statusCode());
        assertEquals(403, get("/facilities/1/edit", cookie).statusCode());
        assertEquals(403, post("/briefs/new", "title=x&date=2026-01-01&text=y", cookie).statusCode());
        verifyNoInteractions(briefs);
    }

    @Test
    void validCreateRedirectsToNewRecord() throws Exception {
        when(briefs.addRecord(any(Brief.class))).thenReturn(new Brief(101, "Op Nightfall", "2026-01-01", "Watch the port.", null));

        HttpResponse<String> response = post("/briefs/new",
                "title=Op+Nightfall&date=2026-01-01&text=Watch+the+port.", login("pat", "Secret1"));

        assertEquals(303, response.statusCode());
        assertEquals("/briefs/101", response.headers().firstValue("Location").orElseThrow());
        verify(audit).log("pat", "CREATE", "BRIEF", "101");
    }

    @Test
    void invalidCreateKeepsTypedValuesAndShowsError() throws Exception {
        HttpResponse<String> response = post("/briefs/new", "title=Op+Nightfall&date=not-a-date&text=Go",
                login("pat", "Secret1"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("value=\"Op Nightfall\""));
        assertTrue(response.body().contains("role=\"alert\""));
        verify(audit, never()).log(anyString(), eq("CREATE"), anyString(), anyString());
    }

    @Test
    void editFormIsFilledWithCurrentValues() throws Exception {
        HttpResponse<String> response = get("/facilities/1/edit", login("pat", "Secret1"));

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("action=\"/facilities/1/edit\""));
        assertTrue(response.body().contains("value=\"Blackwood Annex\""));
        assertTrue(response.body().contains("value=\"BA\""));
    }

    @Test
    void editingUnknownRecordIsNotFound() throws Exception {
        assertEquals(404, get("/facilities/99/edit", login("pat", "Secret1")).statusCode());
    }

    @Test
    void validEditRedirectsToRecordAndLogsUpdate() throws Exception {
        HttpResponse<String> response = post("/facilities/1/edit", "name=Blackwood+Annex&abbreviation=BWA",
                login("pat", "Secret1"));

        assertEquals(303, response.statusCode());
        assertEquals("/facilities/1", response.headers().firstValue("Location").orElseThrow());
        verify(audit).log("pat", "UPDATE", "FACILITY", "1");
    }

    @Test
    void onlyAdminsSeeNewAndEditLinks() throws Exception {
        String admin = login("pat", "Secret1");
        String agent = login("agent7", "Agent77");

        assertTrue(get("/facilities", admin).body().contains("href=\"/facilities/new\""));
        assertTrue(get("/facilities/1", admin).body().contains("href=\"/facilities/1/edit\""));
        assertFalse(get("/facilities/99", admin).body().contains("/edit\""));
        assertFalse(get("/facilities", agent).body().contains("/new\""));
        assertFalse(get("/facilities/1", agent).body().contains("/edit\""));
    }
}
