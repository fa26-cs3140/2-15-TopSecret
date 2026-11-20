package org.managers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.model.*;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountConsistencyIntegrationTest {
    @TempDir Path directory;
    private Cipher cipher() {
        String key = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:";
        return new Cipher(new CipherKey(key + "\n" + key.substring(1) + key.charAt(0)));
    }
    @Test
    void failedFirstPasswordWriteLeavesBootstrapAvailableForRetry() {
        String url = "jdbc:sqlite:" + directory.resolve("db.sqlite");
        FileHandler failing = new FileHandler(directory.toString()) {
            @Override public boolean writeFile(String name, String value) { return false; }
        };
        var failed = new UserAccountManager(url, failing, cipher());
        assertThrows(IllegalStateException.class, () -> failed.createFirstUser("admin", "Secret1"));
        assertFalse(failed.hasUsers());
        var retry = new UserAccountManager(url, new FileHandler(directory.toString()), cipher());
        retry.createFirstUser("admin", "Secret1");
        assertTrue(retry.verifyLogin("admin", "Secret1"));
    }
    @Test
    void separateStoresCannotInsertCaseDuplicateAccounts() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("db.sqlite");
        var first = new SqliteUserAccountStore(url);
        var second = new SqliteUserAccountStore(url);
        try (var workers = Executors.newFixedThreadPool(2)) {
            var a = workers.submit(() -> first.addUser(new UserAccount("Ghost", false)));
            var b = workers.submit(() -> second.addUser(new UserAccount("ghost", false)));
            assertNotEquals(a.get(), b.get());
        }
        assertEquals(1, first.getUsers().size());
    }
    @Test
    void credentialCleanupIsIdempotentAndReportsAnUndeletableDirectory() throws Exception {
        var files = new FileHandler(directory.toString());
        Files.writeString(directory.resolve("partial.cip"), "partial");
        assertTrue(files.deleteFile("partial.cip"));
        assertTrue(files.deleteFile("partial.cip"));
        Files.createDirectories(directory.resolve("nonempty"));
        Files.writeString(directory.resolve("nonempty/child"), "data");
        assertFalse(files.deleteFile("nonempty"));
    }
}
