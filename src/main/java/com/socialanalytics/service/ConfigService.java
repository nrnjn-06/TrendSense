package com.socialanalytics.service;

import com.socialanalytics.model.Platform;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage runtime configuration from environment variables,
 * .env file, and application.properties with strict precedence and safe diagnostics.
 *
 * Precedence Order:
 * 1. System Environment Variables (System.getenv)
 * 2. JVM System Properties (System.getProperty)
 * 3. .env File (checked in multiple standard & packaged launch locations)
 * 4. Classpath application.properties
 */
public class ConfigService {
    private static final ConfigService INSTANCE = new ConfigService();

    private final Properties properties = new Properties();
    private final Map<String, String> propertySources = new ConcurrentHashMap<>();
    private final Map<String, String> runtimeOverrides = new ConcurrentHashMap<>();
    private String loadedEnvFilePath = null;

    private ConfigService() {
        loadProperties();
    }

    public static ConfigService getInstance() {
        return INSTANCE;
    }

    /**
     * Reloads configuration from classpath and file sources.
     * Useful when .env or environment properties are modified while the app is running.
     */
    public synchronized void reload() {
        properties.clear();
        propertySources.clear();
        loadedEnvFilePath = null;
        loadProperties();
    }

    private void loadProperties() {
        // 1. Load application.properties from classpath
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                Properties appProps = new Properties();
                appProps.load(in);
                for (String rawKey : appProps.stringPropertyNames()) {
                    String key = rawKey.trim();
                    String val = cleanValue(appProps.getProperty(rawKey));
                    if (val != null && !val.isBlank()) {
                        properties.setProperty(key, val);
                        propertySources.put(key, "application.properties");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load application.properties: " + e.getMessage());
        }

        // 2. Discover .env file across standard running directories and packaged JAR paths
        List<File> candidateFiles = new ArrayList<>();

        // Explicit override path via property or env var
        String explicitPath = System.getProperty("env.file", System.getenv("ENV_FILE"));
        if (explicitPath == null || explicitPath.isBlank()) {
            explicitPath = System.getProperty("dotenv.path", System.getenv("DOTENV_PATH"));
        }
        if (explicitPath != null && !explicitPath.isBlank()) {
            candidateFiles.add(new File(explicitPath.trim()));
        }

        // Current directory and working directory
        candidateFiles.add(new File(".env"));
        candidateFiles.add(new File(System.getProperty("user.dir", "."), ".env"));

        // Packaged JAR / CodeSource directory probe
        try {
            java.security.CodeSource cs = ConfigService.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                URI csUri = cs.getLocation().toURI();
                File csFile = new File(csUri);
                File csDir = csFile.isDirectory() ? csFile : csFile.getParentFile();
                if (csDir != null) {
                    candidateFiles.add(new File(csDir, ".env"));
                    if (csDir.getParentFile() != null) {
                        candidateFiles.add(new File(csDir.getParentFile(), ".env"));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Parent and user home
        candidateFiles.add(new File(System.getProperty("user.dir", "."), "../.env"));
        candidateFiles.add(new File(System.getProperty("user.home"), ".env"));

        File envFile = null;
        for (File candidate : candidateFiles) {
            if (candidate != null && candidate.exists() && candidate.isFile() && candidate.canRead()) {
                envFile = candidate;
                break;
            }
        }

        if (envFile != null) {
            this.loadedEnvFilePath = envFile.getAbsolutePath();
            try (FileInputStream fis = new FileInputStream(envFile)) {
                Properties envProps = new Properties();
                envProps.load(fis);
                for (String rawKey : envProps.stringPropertyNames()) {
                    String key = rawKey.trim();
                    String val = cleanValue(envProps.getProperty(rawKey));
                    if (val != null && !val.isBlank()) {
                        properties.setProperty(key, val);
                        propertySources.put(key, ".env (" + envFile.getName() + ")");
                    }
                }
            } catch (IOException e) {
                System.err.println("Warning: Could not load .env file from " + envFile.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        // 3. Re-apply any explicit runtime overrides
        for (Map.Entry<String, String> entry : runtimeOverrides.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
            propertySources.put(entry.getKey(), "runtime-override");
        }
    }

    private static String cleanValue(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            if (trimmed.length() >= 2) {
                trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }

    /**
     * Resolves a key checking direct and known alias configurations.
     */
    public String get(String key, String defaultValue) {
        String direct = getDirect(key);
        if (direct != null && !direct.isBlank()) {
            return direct;
        }

        // Check platform and OAuth aliases
        for (String alias : getAliasesForKey(key)) {
            String aliasVal = getDirect(alias);
            if (aliasVal != null && !aliasVal.isBlank()) {
                return aliasVal;
            }
        }

        return defaultValue;
    }

    public String[] getAliasesForKey(String key) {
        if (key == null) return new String[0];
        String normalized = key.toLowerCase().replace('_', '.');
        if (normalized.equals("x.client.id") || normalized.equals("twitter.client.id")) {
            return new String[] {"X_CLIENT_ID", "TWITTER_CLIENT_ID", "x.client.id", "twitter.client.id", "x_client_id", "twitter_client_id"};
        }
        if (normalized.equals("x.client.secret") || normalized.equals("twitter.client.secret")) {
            return new String[] {"X_CLIENT_SECRET", "TWITTER_CLIENT_SECRET", "x.client.secret", "twitter.client.secret", "x_client_secret", "twitter_client_secret"};
        }
        if (normalized.equals("x.redirect.uri") || normalized.equals("twitter.redirect.uri")) {
            return new String[] {"X_REDIRECT_URI", "TWITTER_REDIRECT_URI", "x.redirect.uri", "twitter.redirect.uri"};
        }
        if (normalized.equals("x.bearer.token") || normalized.equals("twitter.bearer.token")) {
            return new String[] {"X_BEARER_TOKEN", "TWITTER_BEARER_TOKEN", "x.bearer.token", "twitter.bearer.token"};
        }
        if (normalized.equals("facebook.app.id") || normalized.equals("meta.app.id") || normalized.equals("facebook.client.id")) {
            return new String[] {"FACEBOOK_APP_ID", "META_APP_ID", "facebook.app.id", "meta.app.id", "facebook.client.id", "FACEBOOK_CLIENT_ID"};
        }
        if (normalized.equals("facebook.app.secret") || normalized.equals("meta.app.secret") || normalized.equals("facebook.client.secret")) {
            return new String[] {"FACEBOOK_APP_SECRET", "META_APP_SECRET", "facebook.app.secret", "meta.app.secret", "facebook.client.secret", "FACEBOOK_CLIENT_SECRET"};
        }
        if (normalized.equals("instagram.client.id") || normalized.equals("instagram.app.id")) {
            return new String[] {"INSTAGRAM_CLIENT_ID", "instagram.client.id", "FACEBOOK_APP_ID", "facebook.app.id", "META_APP_ID", "meta.app.id"};
        }
        if (normalized.equals("instagram.client.secret") || normalized.equals("instagram.app.secret")) {
            return new String[] {"INSTAGRAM_CLIENT_SECRET", "instagram.client.secret", "FACEBOOK_APP_SECRET", "facebook.app.secret", "META_APP_SECRET", "meta.app.secret"};
        }
        if (normalized.equals("linkedin.client.id")) {
            return new String[] {"LINKEDIN_CLIENT_ID", "linkedin.client.id"};
        }
        if (normalized.equals("linkedin.client.secret")) {
            return new String[] {"LINKEDIN_CLIENT_SECRET", "linkedin.client.secret"};
        }
        return new String[0];
    }

    private String getDirect(String key) {
        if (key == null) return null;
        String envKey = key.toUpperCase().replace('.', '_');

        // 1. Check System Environment variable (highest precedence)
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return cleanValue(envVal);
        }
        envVal = System.getenv(key);
        if (envVal != null && !envVal.isBlank()) {
            return cleanValue(envVal);
        }

        // 2. Check JVM System property
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isBlank()) {
            return cleanValue(sysProp);
        }
        sysProp = System.getProperty(envKey);
        if (sysProp != null && !sysProp.isBlank()) {
            return cleanValue(sysProp);
        }

        // 3. Check loaded properties map (.env and application.properties)
        String propVal = properties.getProperty(key);
        if (propVal != null && !propVal.isBlank()) {
            return cleanValue(propVal);
        }
        propVal = properties.getProperty(envKey);
        if (propVal != null && !propVal.isBlank()) {
            return cleanValue(propVal);
        }
        propVal = properties.getProperty(key.toLowerCase().replace('_', '.'));
        if (propVal != null && !propVal.isBlank()) {
            return cleanValue(propVal);
        }

        return null;
    }

    /**
     * Safe provenance report for a configuration key without exposing its value.
     */
    public ConfigSourceInfo getSourceInfo(String key) {
        if (key == null) return new ConfigSourceInfo(false, null, "None");

        List<String> keysToCheck = new ArrayList<>();
        keysToCheck.add(key);
        Collections.addAll(keysToCheck, getAliasesForKey(key));

        for (String candidate : keysToCheck) {
            String envKey = candidate.toUpperCase().replace('.', '_');
            String envVal = System.getenv(envKey);
            if (envVal != null && !envVal.isBlank()) {
                return new ConfigSourceInfo(true, candidate, "Environment Variable (" + envKey + ")");
            }
            envVal = System.getenv(candidate);
            if (envVal != null && !envVal.isBlank()) {
                return new ConfigSourceInfo(true, candidate, "Environment Variable (" + candidate + ")");
            }

            String sysVal = System.getProperty(candidate);
            if (sysVal != null && !sysVal.isBlank()) {
                return new ConfigSourceInfo(true, candidate, "System Property (-D" + candidate + ")");
            }
            sysVal = System.getProperty(envKey);
            if (sysVal != null && !sysVal.isBlank()) {
                return new ConfigSourceInfo(true, candidate, "System Property (-D" + envKey + ")");
            }

            if (properties.containsKey(candidate) && !properties.getProperty(candidate).isBlank()) {
                String src = propertySources.getOrDefault(candidate, "properties");
                return new ConfigSourceInfo(true, candidate, src);
            }
            if (properties.containsKey(envKey) && !properties.getProperty(envKey).isBlank()) {
                String src = propertySources.getOrDefault(envKey, "properties");
                return new ConfigSourceInfo(true, candidate, src);
            }
        }

        return new ConfigSourceInfo(false, key, "Not Configured");
    }

    /**
     * Safe platform diagnostic summary that reveals variable presence, resolution sources,
     * and .env file discovery without leaking secrets.
     */
    public SafePlatformDiagnostics getPlatformDiagnostics(Platform platform) {
        ConfigSourceInfo clientIdInfo = getSourceInfo(platform.getClientIdConfigKey());
        ConfigSourceInfo clientSecretInfo = getSourceInfo(platform.getClientSecretConfigKey());
        ConfigSourceInfo bearerInfo = getSourceInfo(platform.name().toLowerCase() + ".bearer.token");
        String redirectUri = get(platform.getRedirectUriConfigKey(), platform.getDefaultRedirectUri());

        boolean configured = clientIdInfo.present() && clientSecretInfo.present();

        return new SafePlatformDiagnostics(
                platform,
                configured,
                clientIdInfo,
                clientSecretInfo,
                bearerInfo,
                redirectUri,
                loadedEnvFilePath
        );
    }

    public String get(String key) {
        return get(key, "");
    }

    public int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, null);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    public void setProperty(String key, String value) {
        if (value == null || value.isBlank()) {
            runtimeOverrides.remove(key);
            properties.remove(key);
            propertySources.remove(key);
        } else {
            String cleaned = cleanValue(value);
            runtimeOverrides.put(key, cleaned);
            properties.setProperty(key, cleaned);
            propertySources.put(key, "runtime-override");
        }
    }

    public String getLoadedEnvFilePath() {
        return loadedEnvFilePath;
    }

    public record ConfigSourceInfo(boolean present, String resolvedKey, String sourceDescription) {}

    public record SafePlatformDiagnostics(
            Platform platform,
            boolean configured,
            ConfigSourceInfo clientIdInfo,
            ConfigSourceInfo clientSecretInfo,
            ConfigSourceInfo bearerTokenInfo,
            String redirectUri,
            String loadedEnvFilePath
    ) {}
}
