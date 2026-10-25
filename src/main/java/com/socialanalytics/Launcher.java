package com.socialanalytics;

/**
 * Standard launcher entry point for IDEs (Eclipse, IntelliJ, NetBeans).
 * 
 * Bypasses JavaFX modular runtime checks by not directly extending Application,
 * allowing instant execution via "Run As -> Java Application" in Eclipse without
 * requiring complex VM arguments or modular exports.
 */
public class Launcher {
    public static void main(String[] args) {
        SocialAnalyticsApp.main(args);
    }
}
