package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class TopSecret
{
    static void main(String[] args) 
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
