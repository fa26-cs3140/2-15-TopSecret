package org.example;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class UserInterface
{
    ProgramControl pControl;

    public UserInterface()
    {
        pControl = new ProgramControl();
    }

    // This will do all the meaningful heavy lifting for displaying and processing
    // user input
    public void runInterface(String[] args)
    {
        // All useful local variables to run this program
        Scanner scanner = new Scanner(System.in);
        int selectedFileNum = 0;
        List<String> fNames = pControl.getAvailableFiles();
        
        if (args.length > 2)
        {
            // No more than three arguments can be thrown
            scanner.close();
            throw new IllegalArgumentException("Exiting program... Too many arguments (3 max).\n");
        }
        // No args, print out directory and await file selection
        // before moving on
        if (args.length == 0)
        {
            // Display menu
            System.out.println("Choose a file option:");
            System.out.println("---------------------");
            for (int i = 0; i < fNames.size(); i++)
            {
                IO.println("" + i + " " + fNames.get(i));
            }
            IO.println("---------------------");
            // Return early, we are done here
            scanner.close();
            return;
        }
        // If at least one arg, user chooses file at once
        if (args.length >= 1)
        {
            // Find the selected file in the first argument
            try
            {
                selectedFileNum = Integer.parseInt(args[0]);
                if (selectedFileNum > fNames.size() - 1)
                {
                    scanner.close();
                    throw new IllegalArgumentException();
                }
            }
            catch (NumberFormatException e)
            {
                scanner.close();
                throw new IllegalArgumentException("Exitng program... First argument must be an integer.\n");
            }
            // Select the cipher we are going to use
            // Read from the integer selected from a args[1],
            // not strictly the number itself.
            int cipIndex = 0;
            if (args.length == 2) 
            {
                try
                {
                    cipIndex = Integer.parseInt(args[1]);
                    if (cipIndex > pControl.getFileContents("key.txt").split("\\R").length - 1)
                    {
                        scanner.close();
                        throw new IllegalArgumentException("Exiting program... Invalid cipher.\n");
                    }
                }
                catch (InputMismatchException e)
                {
                    scanner.close();
                    throw new IllegalArgumentException("Exitng program... Second argument must be an integer.\n");
                }
            }

            // Find and decipher the text
            String selectedFileName = fNames.get(selectedFileNum);
            String content = pControl.getFileContents(selectedFileName, cipIndex); 

            // Display content
            System.out.println("File Conents: ");
            System.out.println("---------------------");
            System.out.println(content);
            System.out.println("---------------------");
        }
        // Close scanner
        scanner.close();
    }
}
