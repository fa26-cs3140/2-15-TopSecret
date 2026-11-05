package org.controller;

import java.util.Arrays;

// Reads the command-line switches that choose between the terminal and web interfaces.
public final class LaunchOptions {

    public static final String WEB_FLAG = "-web";

    private LaunchOptions() {
    }

    public static boolean isWebMode(String[] args) {
        return args != null && Arrays.asList(args).contains(WEB_FLAG);
    }
}
