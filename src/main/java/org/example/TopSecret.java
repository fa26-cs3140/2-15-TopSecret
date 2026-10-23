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
        UserAccountManager accounts = new UserAccountManager(dbManager, fHandler, createCipher(fHandler));
        Scanner scanner = new Scanner(System.in);


        // uInterface can throw many types of errors
        // Catch if anything happens
        try
        {
            UserAccount user = UserInterface.login(accounts, scanner, System.out);
            if (user == null)
            {
                return;
            }
            UserInterface uInterface = new UserInterface(dbManager, auditLogger, user, scanner, System.out);
            pControl.run(uInterface, args);
        }
        catch (Exception e)
        {
            System.out.println("Error... Aborting program...");
        }
    }

    // Passwords are ciphered with the first mapping in key.txt; falls back to no change if it is missing.
    private static Cipher createCipher(FileHandler fileHandler)
    {
        String keyFile = fileHandler.readFile("key.txt");
        String[] lines = keyFile == null ? new String[0] : keyFile.split("\\R");
        String key = lines.length >= 2
                ? lines[0] + "\n" + lines[1]
                : "abcdefghijklmnopqrstuvwxyz\nabcdefghijklmnopqrstuvwxyz";
        return new Cipher(new CipherKey(key));
    }
}
