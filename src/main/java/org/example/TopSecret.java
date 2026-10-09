package org.example;

public class TopSecret
{
    public static void main(String[] args)
    {
        // Startup to make sure database file exists within the program

        UserInterface uInterface = new UserInterface();
        FileHandler fHandler = new FileHandler();
        DatabaseManager dbManager = new DatabaseManager();
        ProgramControl pControl = new ProgramControl(fHandler, dbManager);


        // uInterface can throw many types of errors
        // Catch if anything happens
        try
        {
            pControl.run(uInterface, args);
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Error... Aborting program...");
        }
    }
}
