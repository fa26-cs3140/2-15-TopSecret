package org.controller;

import org.managers.AgentManager;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.AuditLogService;
import org.model.UserAccount;
import org.managers.UserAccountManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.model.Brief;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Starts a real server on a test port so 3002 stays free for the running application.
class WebServerIntegrationTest {

    private static final int TEST_PORT = 3012;

    private UserAccountManager accounts;
    private WebServer server;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @BeforeEach
    void setUp() throws Exception {
        accounts = mock(UserAccountManager.class);
        when(accounts.hasUsers()).thenReturn(true);
        when(accounts.getUser("pat")).thenReturn(new UserAccount("pat", true));
        when(accounts.passwordExists("pat")).thenReturn(true);
        when(accounts.verifyLogin("pat", "Secret1")).thenReturn(true);
        when(accounts.getUser("agent7")).thenReturn(new UserAccount("agent7", false));
        when(accounts.verifyLogin("agent7", "Agent77")).thenReturn(true);

        BriefManager briefs = mock(BriefManager.class);
        when(briefs.getRecords("id", "%")).thenReturn(List.of(
                new Brief(1, "Operation <Sandtrap>", "1970-11-03", "Bug the pouch.", null)));
        AppController controller = new AppController(briefs, mock(AgentManager.class),
                mock(FacilityManager.class), mock(AuditLogService.class));
        server = new WebServer(controller, accounts, new SessionManager(), TEST_PORT);
        server.start();
    }

    private static AppController newController() {
        return new AppController(mock(BriefManager.class), mock(AgentManager.class),
                mock(FacilityManager.class), mock(AuditLogService.class));
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

    private HttpResponse<String> login(String username, String password) throws Exception {
        String form = "username=" + username + "&password=" + password;
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + TEST_PORT + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String sessionCookie(HttpResponse<String> response) {
        String header = response.headers().firstValue("Set-Cookie").orElseThrow();
        return header.split(";")[0];
    }

    @Test
    void rootShowsLoginForm() throws Exception {
        HttpResponse<String> response = get("/", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<form"));
        assertTrue(response.body().contains("name=\"username\""));
        assertTrue(response.body().contains("type=\"password\""));
    }

    @Test
    void validLoginSetsSessionCookieThatExpiresWithBrowser() throws Exception {
        HttpResponse<String> response = login("pat", "Secret1");

        String header = response.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(header.startsWith("session="));
        assertFalse(header.contains("Expires"));
        assertFalse(header.contains("Max-Age"));
    }

    @Test
    void invalidLoginShowsErrorWithoutCookie() throws Exception {
        HttpResponse<String> response = login("pat", "wrong");

        assertTrue(response.body().contains("Incorrect username or password"));
        assertTrue(response.headers().firstValue("Set-Cookie").isEmpty());
    }

    @Test
    void pagesRedirectToLoginWithoutCookie() throws Exception {
        for (String path : new String[]{"/menu", "/briefs", "/agents", "/facilities", "/audit"}) {
            HttpResponse<String> response = get(path, null);

            assertEquals(303, response.statusCode(), path);
            assertEquals("/", response.headers().firstValue("Location").orElseThrow(), path);
        }
    }

    @Test
    void pagesLoadWithSessionCookie() throws Exception {
        String cookie = sessionCookie(login("pat", "Secret1"));

        assertEquals(200, get("/menu", cookie).statusCode());
        assertEquals(200, get("/briefs", cookie).statusCode());
    }

    @Test
    void fakeCookieIsRejected() throws Exception {
        assertEquals(303, get("/menu", "session=made-up").statusCode());
    }

    @Test
    void logoutEndsSession() throws Exception {
        String cookie = sessionCookie(login("pat", "Secret1"));

        get("/logout", cookie);

        assertEquals(303, get("/menu", cookie).statusCode());
    }

    @Test
    void serverDoesNotStartWithoutAccounts() {
        when(accounts.hasUsers()).thenReturn(false);
        AppController controller = newController();
        WebServer empty = new WebServer(controller, accounts, new SessionManager(), TEST_PORT + 1);

        assertThrows(IllegalStateException.class, empty::start);
    }

    @Test
    void briefListLinksToRecordPagesAndEscapesHtml() throws Exception {
        String cookie = sessionCookie(login("pat", "Secret1"));

        String body = get("/briefs", cookie).body();

        assertTrue(body.contains("<a href=\"/briefs/1\">1. Operation &lt;Sandtrap&gt;</a>"));
    }

    @Test
    void recordPageShowsDetails() throws Exception {
        String cookie = sessionCookie(login("pat", "Secret1"));

        String body = get("/briefs/1", cookie).body();

        assertTrue(body.contains("<p>Date: 1970-11-03</p>"));
        assertTrue(body.contains("<p>Text: Bug the pouch.</p>"));
    }

    @Test
    void unknownPagesAndBadIdsReturnNotFound() throws Exception {
        String cookie = sessionCookie(login("pat", "Secret1"));

        assertEquals(404, get("/nothing", cookie).statusCode());
        assertEquals(404, get("/briefs/abc", cookie).statusCode());
        assertEquals(404, get("/briefs/1/extra", cookie).statusCode());
    }

    @Test
    void standardUserCannotSeeAuditLog() throws Exception {
        String cookie = sessionCookie(login("agent7", "Agent77"));

        assertFalse(get("/menu", cookie).body().contains("/audit"));
        assertTrue(get("/audit", cookie).body().contains("Access denied"));
    }

    @Test
    void loggedInUserVisitingRootGoesToMenu() throws Exception {
        String cookie = sessionCookie(login("pat", "Secret1"));

        HttpResponse<String> response = get("/", cookie);

        assertEquals(303, response.statusCode());
        assertEquals("/menu", response.headers().firstValue("Location").orElseThrow());
    }
}
