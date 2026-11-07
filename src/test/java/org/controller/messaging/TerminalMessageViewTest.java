package org.controller.messaging;
import org.junit.jupiter.api.Test;
import org.view.messaging.TerminalMessageView;
import java.io.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class TerminalMessageViewTest {
 @Test void validatesOutputAndRendersEmptyAndErrorStates(){assertThrows(IllegalArgumentException.class,()->new TerminalMessageView(null));var bytes=new ByteArrayOutputStream();var view=new TerminalMessageView(new PrintStream(bytes));view.showAgents(List.of());view.showInbox(List.of());view.showError("No such agent");assertTrue(bytes.toString().contains("No agents"));assertTrue(bytes.toString().contains("no messages"));assertTrue(bytes.toString().contains("No such agent"));}
}
