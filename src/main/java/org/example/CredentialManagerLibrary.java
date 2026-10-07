package org.example;

public class CredentialManagerLibrary implements CredentialManager {

    private FileHandler fileHandler;
    private Cipher cipher;

    public CredentialManagerLibrary(FileHandler fileHandler, Cipher cipher) {
        this.fileHandler = fileHandler;
        this.cipher = cipher;
    }

    @Override
    public boolean credentialsExist() {
        return fileHandler.getAvailableFiles().contains("credentials.cip");
    }

    @Override
    public boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        char[] CharArray =  username.toCharArray();
        for (char c : CharArray) {
            if (!Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }
    @Override
    public boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        if (password.length() < 5) {
            return false;
        }
        return true;
    }

    @Override
    public boolean verifyLogin(String username, String password) {
        String cipheredCredentials = fileHandler.readFile("credentials.cip");
        if (cipheredCredentials == null) { return false; }
        String credentials = cipher.decipher(cipheredCredentials);
        String combined = username + "\n" + password;
        if (combined.equals(credentials)) {
            return true;
        }
        else { return false; }
    }

    @Override
    public boolean createCredentials(String username, String password) {
        if (isValidUsername(username) == true && isValidPassword(password) == true) {
            String combined = username + "\n" + password;
            String cipheredCredentials = cipher.cipher(combined);
            fileHandler.writeFile("credentials.cip", cipheredCredentials);
            return true;
        }
        return false;

    }

    @Override
    public boolean changePassword(String username, String newPassword) {
        if (isValidPassword(newPassword) == true) {
            String combined = username + "\n" + newPassword;
            String cipheredCredentials = cipher.cipher(combined);
            fileHandler.writeFile("credentials.cip", cipheredCredentials);
            return true;
        }
        else {
            return false;
        }
    }
}
