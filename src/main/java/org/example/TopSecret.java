package org.example;

public class TopSecret
{
    public static void main(String[] args)
    {
        UserInterface uInterface = new UserInterface();

        // uInterface can throw many types of errors
        // Catch if anything happens
        try
        {
            uInterface.runInterface(args);
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Error... Aborting program...");
        }
    }
}
