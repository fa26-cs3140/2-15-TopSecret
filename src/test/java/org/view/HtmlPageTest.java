package org.view;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlPageTest {

    @Test
    void escapeReplacesHtmlCharacters() {
        assertEquals("&lt;b&gt; &amp; &quot;x&quot;", HtmlPage.escape("<b> & \"x\""));
    }

    @Test
    void pageHasTitleAndNoStylingOrScripts() {
        String html = HtmlPage.page("Briefs", "<p>Body</p>", true, false);

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<title>Top Secret - Briefs</title>"));
        assertTrue(html.contains("<p>Body</p>"));
        assertFalse(html.contains("<style"));
        assertFalse(html.contains("<script"));
    }

    @Test
    void navigationShowsAuditLinkOnlyForAdmins() {
        assertTrue(HtmlPage.page("Menu", "", true, true).contains("href=\"/audit\""));
        assertFalse(HtmlPage.page("Menu", "", true, false).contains("href=\"/audit\""));
    }

    @Test
    void loginPageHasNoNavigation() {
        assertFalse(HtmlPage.page("Log in", "", false, false).contains("href=\"/logout\""));
    }

    @Test
    void listMakesOneItemPerLineWithOptionalLinks() {
        String html = HtmlPage.list("1. Ghost\nNo link", Arrays.asList("/agents/1", null));

        assertTrue(html.contains("<li><a href=\"/agents/1\">1. Ghost</a></li>"));
        assertTrue(html.contains("<li>No link</li>"));
    }

    @Test
    void listWithoutLinksShowsPlainItems() {
        assertEquals("<ul>\n<li>a</li>\n<li>b</li>\n</ul>", HtmlPage.list("a\nb", null));
    }

    @Test
    void paragraphsMakeOneParagraphPerLine() {
        assertEquals("<p>Name: Ghost</p>\n<p>Notes: N/A</p>\n", HtmlPage.paragraphs("Name: Ghost\nNotes: N/A"));
    }

    @Test
    void loginFormShowsErrorOnlyWhenGiven() {
        assertTrue(HtmlPage.loginForm("Bad login").contains("<strong>Bad login</strong>"));
        assertFalse(HtmlPage.loginForm(null).contains("<strong>"));
        assertTrue(HtmlPage.loginForm(null).contains("method=\"post\""));
    }

    @Test
    void listHandlesShortLinkList() {
        assertTrue(HtmlPage.list("a\nb", List.of("/x")).contains("<li>b</li>"));
    }
}
