package org.model.messaging;
import org.managers.*;
import org.model.*;
import java.time.LocalDateTime;
import java.util.List;

/** Construct only after the application's login flow has verified the account.
 * Captures the username, not the mutable UserAccount object. No admin override.
 */
public final class AuthenticatedMessageService implements MessageService {
 private final AgentManager agents;
 private final UserAccountStore users;
 private final MessageStore store;
 private final String username;
 public AuthenticatedMessageService(AgentManager agents,UserAccountStore users,MessageStore store,UserAccount authenticatedUser){
  if(agents==null||users==null||store==null||authenticatedUser==null||authenticatedUser.getUsername()==null)
   throw new IllegalArgumentException("Authenticated messaging dependencies required.");
  this.agents=agents;this.users=users;this.store=store;this.username=authenticatedUser.getUsername();
  requireAccount();
 }
 private void requireAccount(){
  if(users.getUser(username)==null)throw new IllegalArgumentException("Authenticated account no longer exists.");
  if(users.getUsers().stream().filter(u->u.getUsername().equalsIgnoreCase(username)).count()!=1)
   throw new IllegalStateException("Ambiguous account identity. Resolve case-duplicate accounts before messaging.");
 }
 @Override public List<Agent> getAgents(){requireAccount();return List.copyOf(agents.getRecords("id","%"));}
 @Override public Message sendMessage(int agentId,String body){
  requireAccount();if(body==null||body.isBlank())throw new IllegalArgumentException("Message cannot be blank.");
  Agent agent=getAgents().stream().filter(a->a.getId()==agentId).findFirst().orElseThrow(()->new IllegalArgumentException("No such agent."));
  return store.save(new Message(0,username,agentId,agent.getName(),body,LocalDateTime.now()));
 }
 @Override public List<Message> getInbox(){requireAccount();return store.findForRecipient(username);}
}
