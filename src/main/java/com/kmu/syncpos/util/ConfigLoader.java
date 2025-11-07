package com.kmu.syncpos.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for loading application configuration from properties file.
 * Provides centralized access to configuration values.
 */
public class ConfigLoader {

    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class.getName());
    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    static {
        loadProperties();
    }

    /**
     * Loads the application.properties file from the classpath.
     */
    private static void loadProperties() {
        if (loaded) {
            return;
        }

        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (input == null) {
                LOGGER.log(Level.WARNING, "Unable to find application.properties file. Using default values.");
                setDefaultProperties();
                return;
            }

            properties.load(input);
            loaded = true;
            LOGGER.log(Level.INFO, "Successfully loaded application configuration");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading application.properties", e);
            setDefaultProperties();
        }
    }

    /**
     * Sets default property values when configuration file cannot be loaded.
     */
    private static void setDefaultProperties() {
        properties.setProperty("supabase.url", "https://tsbanhacgiqxilvfydpf.supabase.co");
        properties.setProperty("supabase.anon.key", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRzYmFuaGFjZ2lxeGlsdmZ5ZHBmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTQxMjAyMTYsImV4cCI6MjA2OTY5NjIxNn0.DlaXJASotyIqFs4bEIk6mbmfyMgsVGllNjOlusSc0Vw");
        properties.setProperty("database.path", "~/.syncpos/syncpos.db");
        loaded = true;
    }

    /**
     * Gets a configuration property value.
     *
     * @param key The property key
     * @return The property value, or null if not found
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Gets a configuration property value with a default fallback.
     *
     * @param key The property key
     * @param defaultValue The default value to return if key not found
     * @return The property value, or defaultValue if not found
     */
    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Gets the Supabase URL from configuration.
     *
     * @return The Supabase URL
     */
    public static String getSupabaseUrl() {
        return getProperty("supabase.url", "https://tsbanhacgiqxilvfydpf.supabase.co");
    }

    /**
     * Gets the Supabase anonymous key from configuration.
     *
     * @return The Supabase anonymous key
     */
    public static String getSupabaseAnonKey() {
        return getProperty("supabase.anon.key", "");
    }

    /**
     * Gets the database path from configuration.
     *
     * @return The database path
     */
    public static String getDatabasePath() {
        return getProperty("database.path", "~/.syncpos/syncpos.db");
    }
}
