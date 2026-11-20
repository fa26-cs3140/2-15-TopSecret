package org.view.messaging;
import org.model.Agent;
import org.model.messaging.Message;
import java.io.PrintStream;
import java.util.List;
/** Plain terminal rendering; message bodies are never copied into public audit history. */
public final class TerminalMessageView implements MessageView {
 private final PrintStream output;
 public TerminalMessageView(PrintStream output){if(output==null)throw new IllegalArgumentException("Output required.");this.output=output;}
 public void showAgents(List<Agent> agents){if(agents.isEmpty())output.println("No agents are available.");agents.forEach(a->output.println(a.getId()+". "+a.getName()));}
 public void showInbox(List<Message> messages){
  if(messages.isEmpty())output.println("You have no messages.");
  for(Message m:messages)output.println("From: "+m.getSenderUsername()+" | To: "+m.getRecipientName()+" | Sent: "+m.getSentAt()+"\n"+m.getBody()+"\n---");
 }
 public void showMessageSent(Message message){output.println("Message sent to "+message.getRecipientName()+".");}
 public void showError(String message){output.println("Unable to send message: "+message);}
}
