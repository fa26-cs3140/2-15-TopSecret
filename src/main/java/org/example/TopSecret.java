package org.example;

import java.util.Scanner;

public class TopSecret
{
    public static void main(String[] args)
    {
        // Startup to make sure database file exists within the program

        // UserInterface uInterface = new UserInterface();
        ProgramControl pControl = new ProgramControl();

        // uInterface can throw many types of errors
        // Catch if anything happens
        try
        {
            pControl.run(args);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
