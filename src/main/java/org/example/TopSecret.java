package org.example;

import java.util.Scanner;

public class TopSecret
{
    public static void main(String[] args)
    {
        // Startup to make sure database file exists within the program

        FileHandler fHandler = new FileHandler();
        DatabaseManager dbManager = new DatabaseManager("jdbc:sqlite:data/mission_briefs.db");
        ProgramControl pControl = new ProgramControl(fHandler, dbManager);


        AuditLogManager auditLogger = new AuditLogManager(dbManager);
        UserInterface uInterface = new UserInterface(dbManager, auditLogger,
                                                     new UserAccount("test", true),
                                                     new Scanner(System.in),
                                                     System.out);


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
