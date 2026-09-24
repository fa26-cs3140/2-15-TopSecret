package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class UserInterface
{
    File filedata;
    FileHandler fHandler;

    public UserInterface()
    {
        // fHandler points to data directory by default
        this.fHandler = new FileHandler();
        this.filedata = null;
    }

    // This will do all the meaningful heavy lifting for displaying and processing
    // user input
    public void runInterface(String[] args)
    {
        // All useful local variables to run this program
        Scanner scanner = new Scanner(System.in);
        int selectedFileNum = 0;
        CipherKey cipKey = null;
        Cipher cipher = null;
        List<String> fNames = fHandler.getAvailableFiles();
        
        // Get map and default key (in key.txt)
        String mapping = fHandler.readFile("key.txt").split("\\R")[0];
        
        if (args.length > 2)
        {
            // No more than three arguments can be thrown
            IO.println("Error: Too many arguments.");
            scanner.close();
            throw new IllegalArgumentException("Exiting program... Too many arguments (3 max).\n");
        }
        // No args, print out directory and await file selection
        // before moving on
        if (args.length == 0)
        {
            // Display menu
            IO.println("Choose a file option:");
            IO.println("---------------------");
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
                throw new IllegalArgumentException("Exiitng program... First argument must be an integer.\n");
            }
            // Select the cipher we are going to use
            // Read from the integer selected from a args[1],
            // not strictly the number itself.
            if (args.length == 2) 
            {
                int cipIndex = 0;
                try
                {
                    cipIndex = Integer.parseInt(args[1]);
                    if (cipIndex > fNames.size() - 1)
                    {
                        scanner.close();
                        throw new IllegalArgumentException();
                    }
                }
                catch (InputMismatchException e)
                {
                    scanner.close();
                    throw new IllegalArgumentException("Exiitng program... Second argument must be an integer.\n");
                }
                String cipMap = fHandler.readFile("key.txt").split("\\R")[cipIndex];
                cipKey = new CipherKey(mapping + "\n" + cipMap);
            }
            // Otherwise 
            else cipKey = new CipherKey(mapping + "\n" + mapping);

            cipher = new Cipher(cipKey);

            // Find and decipher the text
            String cipheredContent = fHandler.readFile(fNames.get(selectedFileNum));
            String decipheredContent = cipher.decipher(cipheredContent);

            // Display content
            IO.println("File Conents: ");
            IO.println("---------------------");
            IO.println(decipheredContent);
            IO.println("---------------------");
        }
        // Close scanner
        scanner.close();
    }
}
