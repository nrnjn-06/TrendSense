package com.socialanalytics.database;

import org.h2.tools.Console;

/**
 * Utility launcher to open the official H2 Web Console in the default browser.
 * Allows viewing all tables, columns, and records in an intuitive web GUI.
 */
public class H2ConsoleLauncher {
    public static void main(String[] args) throws Exception {
        System.out.println("=================================================================");
        System.out.println("🌐 Launching H2 Database Web Console...");
        System.out.println("👉 In your browser window, ensure these connection settings are entered:");
        System.out.println("   - Driver Class : org.h2.Driver");
        System.out.println("   - JDBC URL     : jdbc:h2:./data/social_analytics;MODE=MySQL;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE");
        System.out.println("   - User Name    : sa");
        System.out.println("   - Password     : (leave completely empty)");
        System.out.println("=================================================================");
        Console.main("-web", "-webPort", "8082", "-browser");
    }
}
