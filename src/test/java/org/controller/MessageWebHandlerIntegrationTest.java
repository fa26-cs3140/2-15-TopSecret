package org.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.managers.AgentManager;
import org.managers.AuditLogService;
import org.managers.BriefManager;
import org.managers.FacilityManager;
import org.managers.MissionManager;
import org.managers.SqliteUserAccountStore;
import org.managers.UserAccountManager;

import org.model.UserAccount;
import org.model.messaging.Message;
import org.model.messaging.SqliteMessageStore;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests web messaging using real HTTP requests and SQLite persistence.
 *
 * Login credentials are mocked, but user identities, agents,
 * messages, and read status are backed by a temporary database.
 */
class MessageWebHandlerIntegrationTest {

    @TempDir
    Path dir;

    private WebServer server;
    private SqliteMessageStore messageStore;
    private int ghostAgentId;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("messages.db");

        // Create the real user accounts.
        SqliteUserAccountStore users = new SqliteUserAccountStore(url);

        users.addUser(new UserAccount("sender", false));
        users.addUser(new UserAccount("ghost", false));
        users.addUser(new UserAccount("stranger", false));

        // Create a facility for the recipient agent.
        FacilityManager facilities = new FacilityManager(url);
        facilities.createTable();

        int facilityId = facilities.addRecord(
                "HQ", "HQ", 1
        ).getId();

        // Create the agent named Ghost.
        AgentManager agents = new AgentManager(url);
        agents.createTable();

        ghostAgentId = agents.addRecord(
                "Ghost",
                "1990-01-01",
                null,
                null,
                facilityId
        ).getId();

        // Initialize real messaging persistence.
        messageStore = new SqliteMessageStore(url);

        MessageWebHandler messaging = new MessageWebHandler(
                agents,
                users,
                messageStore
        );

        // Mock the application's password authentication.
        UserAccountManager accounts = mock(UserAccountManager.class);

        when(accounts.hasUsers()).thenReturn(true);

        for (String username : List.of("sender", "ghost", "stranger")) {
            when(accounts.getUser(username)).thenReturn(users.getUser(username));

            when(accounts.verifyLogin(username, "password")).thenReturn(true);
        }

        // Other application features are not relevant to these tests.
        AppController controller = new AppController(
                mock(BriefManager.class),
                agents,
                mock(MissionManager.class),
                facilities,
                mock(AuditLogService.class)
        );

        // Port 0 lets the OS select an available test port.
        server = new WebServer(
                controller,
                accounts,
                new SessionManager(),
                0,
                messaging
        );

        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    /** Sends a GET request with an optional session cookie. */
    private HttpResponse<String> get(String path, String cookie)
            throws Exception {

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + path));

        if (cookie != null) {
            request.header("Cookie", cookie);
        }

        return client.send(
                request.GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    /** Sends a URL-encoded POST request. */
    private HttpResponse<String> post(
            String path,
            String cookie,
            String form) throws Exception {

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + path))
                .header("Content-Type", "application/x-www-form-urlencoded");

        if (cookie != null) {
            request.header("Cookie", cookie);
        }

        return client.send(
                request.POST(HttpRequest.BodyPublishers.ofString(form)).build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    /** Encodes a value for an HTML form submission. */
    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Logs in as a user and returns their session cookie. */
    private String login(String username) throws Exception {
        HttpResponse<String> response = post(
                "/login",
                null,
                "username=" + encode(username) + "&password=password"
        );

        assertEquals(303, response.statusCode());

        return response.headers()
                .firstValue("Set-Cookie")
                .orElseThrow()
                .split(";")[0];
    }

    /** Sends a message as the specified logged-in user. */
    private HttpResponse<String> sendMessage(
            String cookie,
            String body) throws Exception {

        String form = "recipientAgentId=" + ghostAgentId + "&body=" + encode(body);

        return post("/messages", cookie, form);
    }

    @Test
    void messagingRequiresAuthentication() throws Exception {
        // Unauthenticated requests should redirect to login.
        assertEquals(303, get("/messages", null).statusCode());
        assertEquals(303, get("/messages/new", null).statusCode());
        assertEquals(303, get("/messages/1", null).statusCode());

        // Sending a message also requires authentication.
        assertEquals(303, post(
                "/messages",
                null,
                "recipientAgentId=1&body=Hello"
        ).statusCode());

        assertTrue(messageStore.findForRecipient("Ghost").isEmpty());
    }

    @Test
    void regularUserCanAccessMessaging() throws Exception {
        String cookie = login("sender");

        HttpResponse<String> inbox = get("/messages", cookie);
        HttpResponse<String> compose = get("/messages/new", cookie);

        assertEquals(200, inbox.statusCode());
        assertEquals(200, compose.statusCode());

        assertTrue(inbox.body().contains("Compose message"));
        assertTrue(compose.body().contains("Select an agent"));
        assertTrue(compose.body().contains("Ghost"));
    }

    @Test
    void sendingMessagePersistsAndRedirects() throws Exception {
        String cookie = login("sender");

        HttpResponse<String> response = sendMessage(
                cookie,
                "Meet at HQ tomorrow"
        );

        // Successful POST redirects instead of rendering the form again.
        assertEquals(303, response.statusCode());
        assertEquals(
                "/messages?sent=1",
                response.headers().firstValue("Location").orElseThrow()
        );

        // Verify the message was actually saved to SQLite.
        List<Message> inbox = messageStore.findForRecipient("Ghost");

        assertEquals(1, inbox.size());
        assertEquals("sender", inbox.get(0).getSenderUsername());
        assertEquals("Meet at HQ tomorrow", inbox.get(0).getBody());
        assertFalse(inbox.get(0).isRead());

        // Verify the redirect destination shows confirmation.
        assertTrue(get("/messages?sent=1", cookie).body().contains("Message sent."));
    }

    @Test
    void inboxDoesNotMarkMessagesAsRead() throws Exception {
        String senderCookie = login("sender");
        String ghostCookie = login("ghost");

        sendMessage(senderCookie, "Unread mission briefing");

        HttpResponse<String> inbox = get("/messages", ghostCookie);

        assertEquals(200, inbox.statusCode());
        assertTrue(inbox.body().contains("Unread"));
        assertTrue(inbox.body().contains("From: sender"));

        // Listing the inbox must not mark the message as read.
        Message saved = messageStore.findForRecipient("Ghost").get(0);
        assertFalse(saved.isRead());
    }

    @Test
    void openingMessageMarksItAsRead() throws Exception {
        String senderCookie = login("sender");
        String ghostCookie = login("ghost");

        sendMessage(senderCookie, "Secret mission details");

        Message saved = messageStore.findForRecipient("Ghost").get(0);

        String path = "/messages/" + saved.getId();

        HttpResponse<String> details = get(path, ghostCookie);

        assertEquals(200, details.statusCode());
        assertTrue(details.body().contains("Secret mission details"));

        // Verify that the message is now read in SQLite.
        assertTrue(messageStore.findForRecipient("Ghost").get(0).isRead());

        // The inbox should now display Read instead of Unread.
        HttpResponse<String> inbox = get("/messages", ghostCookie);

        assertTrue(inbox.body().contains("message-item--read"));
        assertFalse(inbox.body().contains("message-item--unread"));
    }

    @Test
    void otherUsersCannotReadRecipientsMessages() throws Exception {
        String senderCookie = login("sender");
        String strangerCookie = login("stranger");

        sendMessage(senderCookie, "Confidential information");

        Message saved = messageStore.findForRecipient("Ghost").get(0);

        // The stranger attempts to open Ghost's message by ID.
        HttpResponse<String> response = get(
                "/messages/" + saved.getId(),
                strangerCookie
        );

        assertEquals(404, response.statusCode());
        assertFalse(response.body().contains("Confidential information"));

        // An unauthorized request must not mark the message as read.
        assertFalse(messageStore.findForRecipient("Ghost").get(0).isRead());

        // Stranger's inbox should also be empty.
        assertTrue(get("/messages", strangerCookie).body().contains("You have no messages."));
    }

    @Test
    void invalidMessageIdsReturnNotFound() throws Exception {
        String cookie = login("ghost");

        assertEquals(404, get("/messages/999999", cookie).statusCode());
        assertEquals(404, get("/messages/abc", cookie).statusCode());
        assertEquals(404, get("/messages/1/extra", cookie).statusCode());
        assertEquals(404, get("/messages/0", cookie).statusCode());
    }

    @Test
    void invalidMessageSubmissionPreservesDraft() throws Exception {
        String cookie = login("sender");

        String draft = "Important <message>";

        HttpResponse<String> response = post(
                "/messages",
                cookie,
                "recipientAgentId=" + ghostAgentId
                        + "&body=" + encode(draft)
                        .replace("Important", "Important")
        );

        // This valid submission should succeed.
        assertEquals(303, response.statusCode());

        // Now submit an invalid agent ID with the same draft.
        HttpResponse<String> invalid = post(
                "/messages",
                cookie,
                "recipientAgentId=999999&body=" + encode(draft)
        );

        assertEquals(400, invalid.statusCode());

        // The form should preserve the escaped draft.
        assertTrue(invalid.body().contains("Important &lt;message&gt;"));

        // No additional message should have been saved.
        assertEquals(1, messageStore.findForRecipient("Ghost").size());
    }

    @Test
    void unsupportedMethodsReturn405() throws Exception {
        String cookie = login("sender");

        // POST is not allowed on the compose page.
        HttpResponse<String> response = post(
                "/messages/new",
                cookie,
                "body=Hello"
        );

        assertEquals(405, response.statusCode());
    }

    @Test
    void messageContentsAreHtmlEscaped() throws Exception {
        String senderCookie = login("sender");
        String ghostCookie = login("ghost");

        sendMessage(senderCookie, "<script>alert('x')</script>");

        Message saved = messageStore.findForRecipient("Ghost").get(0);

        HttpResponse<String> response = get(
                "/messages/" + saved.getId(),
                ghostCookie
        );

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("&lt;script&gt;"));
        assertFalse(response.body().contains("<script>"));
    }
}