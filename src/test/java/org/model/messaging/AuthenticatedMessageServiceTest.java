package org.model.messaging;
import org.junit.jupiter.api.Test;
import org.managers.*;
import org.model.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AuthenticatedMessageServiceTest {
 @Test void identityIsCapturedAndNeverTakenFromEditableUserObject(){var users=mock(UserAccountStore.class);var agents=mock(AgentManager.class);var store=mock(MessageStore.class);var account=new UserAccount("ghost",false);
 when(users.getUser("ghost")).thenReturn(new UserAccount("ghost",false));when(users.getUsers()).thenReturn(List.of(new UserAccount("ghost",false)));
 var service=new AuthenticatedMessageService(agents,users,store,account);account.setUsername("admin");service.getInbox();verify(store).findForRecipient("ghost");}
 @Test void rejectsNullDependenciesAndDeletedAccounts(){assertThrows(IllegalArgumentException.class,()->new AuthenticatedMessageService(null,null,null,null));var users=mock(UserAccountStore.class);assertThrows(IllegalArgumentException.class,()->new AuthenticatedMessageService(mock(AgentManager.class),users,mock(MessageStore.class),new UserAccount("missing",false)));}
}
