package org.example;

public class TopSecret
{
    public static void main(String[] args)
    {
        // Startup to make sure database file exists within the program

        UserInterface uInterface = new UserInterface();
        FileHandler fHandler = new FileHandler();
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:data/mission_briefs.db");
        ProgramControl pControl = new ProgramControl(fHandler, dbManager);

        AuditLogManager auditLogManager = new AuditLogManager(dbManager);


        // uInterface can throw many types of errors
        // Catch if anything happens
        try
        {
            pControl.run(uInterface, args);
        }
        catch (Exception e)
        {
            System.out.println("Error... Aborting program...");
        }
    }
}
