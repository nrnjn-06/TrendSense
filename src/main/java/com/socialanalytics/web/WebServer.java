package com.socialanalytics.web;

import com.socialanalytics.database.DatabaseManager;
import com.socialanalytics.servlet.*;
import java.io.File;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

/**
 * Embedded Tomcat runner providing zero-setup local execution for
 * Jakarta Servlets and JSP web application.
 */
public class WebServer {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        // Initialize database schema
        DatabaseManager.getInstance();

        int port = DEFAULT_PORT;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try {
                port = Integer.parseInt(portEnv.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // trigger default HTTP connector

        File baseDir = new File("target/tomcat." + port);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        tomcat.setBaseDir(baseDir.getAbsolutePath());

        File webappDir = new File("src/main/webapp");
        if (!webappDir.exists()) {
            webappDir = new File("webapp");
        }

        Context ctx = tomcat.addWebapp("", webappDir.getAbsolutePath());
        ctx.setParentClassLoader(WebServer.class.getClassLoader());
        ctx.setAllowCasualMultipartParsing(true);

        // Explicitly register servlets for instant execution
        registerServlets(ctx);

        System.out.println("=================================================================");
        System.out.println("🚀 Social Analytics Web Server starting on port " + port);
        System.out.println("👉 Access the web application at: http://localhost:" + port + "/");
        System.out.println("   Available web endpoints:");
        System.out.println("   - http://localhost:" + port + "/dashboard");
        System.out.println("   - http://localhost:" + port + "/posts");
        System.out.println("   - http://localhost:" + port + "/trends");
        System.out.println("   - http://localhost:" + port + "/sentiment");
        System.out.println("   - http://localhost:" + port + "/best-time");
        System.out.println("   - http://localhost:" + port + "/recommendations");
        System.out.println("   - http://localhost:" + port + "/connect");
        System.out.println("   - http://localhost:" + port + "/login");
        System.out.println("=================================================================");

        tomcat.start();
        tomcat.getServer().await();
    }

    private static void registerServlets(Context ctx) {
        // Dashboard
        Tomcat.addServlet(ctx, "DashboardServlet", new DashboardServlet());
        ctx.addServletMappingDecoded("/dashboard", "DashboardServlet");
        ctx.addServletMappingDecoded("", "DashboardServlet");

        // Auth
        Tomcat.addServlet(ctx, "LoginServlet", new LoginServlet());
        ctx.addServletMappingDecoded("/login", "LoginServlet");

        Tomcat.addServlet(ctx, "RegisterServlet", new RegisterServlet());
        ctx.addServletMappingDecoded("/register", "RegisterServlet");

        Tomcat.addServlet(ctx, "LogoutServlet", new LogoutServlet());
        ctx.addServletMappingDecoded("/logout", "LogoutServlet");

        // Features
        Tomcat.addServlet(ctx, "PostServlet", new PostServlet());
        ctx.addServletMappingDecoded("/posts", "PostServlet");

        Tomcat.addServlet(ctx, "TrendServlet", new TrendServlet());
        ctx.addServletMappingDecoded("/trends", "TrendServlet");

        Tomcat.addServlet(ctx, "SentimentServlet", new SentimentServlet());
        ctx.addServletMappingDecoded("/sentiment", "SentimentServlet");

        Tomcat.addServlet(ctx, "RecommendationServlet", new RecommendationServlet());
        ctx.addServletMappingDecoded("/recommendations", "RecommendationServlet");

        Tomcat.addServlet(ctx, "BestTimeServlet", new BestTimeServlet());
        ctx.addServletMappingDecoded("/best-time", "BestTimeServlet");

        Tomcat.addServlet(ctx, "ConnectAccountServlet", new ConnectAccountServlet());
        ctx.addServletMappingDecoded("/connect", "ConnectAccountServlet");

        Tomcat.addServlet(ctx, "OAuthCallbackServlet", new OAuthCallbackServlet());
        ctx.addServletMappingDecoded("/oauth/callback", "OAuthCallbackServlet");
        ctx.addServletMappingDecoded("/oauth/callback/*", "OAuthCallbackServlet");

        Tomcat.addServlet(ctx, "SyncServlet", new SyncServlet());
        ctx.addServletMappingDecoded("/sync", "SyncServlet");
    }
}
