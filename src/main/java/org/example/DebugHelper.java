package org.example;

// This is a helper class with some useful debug functions I've created. 
public class DebugHelper
{
    // Prints out a message but also includes the name of the
    // method it printed out from
    public static void debugPrintln(String text)
    {
        // Pop 2 to acount for depth
        String currentMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
        System.out.println("[" + currentMethod + "]: " + text);
    }

    public static void debugPrintArray(Object[] arr)
    {
        String currentMethod = Thread.currentThread().getStackTrace()[2].getMethodName();
        for (int i = 0; i < arr.length; i++)
        {
            System.out.println("[" + currentMethod + "]: " + i + " : "+ arr[i].toString());
        }
    }
}
