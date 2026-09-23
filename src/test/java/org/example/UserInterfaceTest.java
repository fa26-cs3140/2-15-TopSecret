package org.example;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class UserInterfaceTest {
    
    UserInterface tUser;
    
    @Test
    void runInterface() 
    {
        tUser = new UserInterface();
        assertDoesNotThrow(()->tUser.runInterface(), "Threw an exception");
    }

    @Test
    void getFileData() 
    {
        tUser = new UserInterface();
        File f = tUser.getFileData();
        if (f == null) throw new NullPointerException("File not found");
    }

    @Test
    void getFileText() 
    {
        tUser = new UserInterface();
        String f_txt = tUser.getFileText();
        if (f_txt == null) throw new NullPointerException("File text not found");
    }


    @Test
    void getCipherKey() 
    {
        tUser = new UserInterface();
        int intendedCipher = (new Random()).nextInt(0, 10);
        int cipher = tUser.getCipherKey();
        assertEquals(cipher, intendedCipher, "Ciphers not equal");
    }

}
