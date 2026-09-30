package com.ocs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Loads config.properties and hands out JDBC connections. */
public final class Database {
    private static final Properties PROPS = new Properties();

    static {
        File f = new File(System.getProperty("ocs.config", "config.properties"));
        try (InputStream in = f.exists() ? new FileInputStream(f) : Database.class.getResourceAsStream("/config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found. Copy "
                        + "src/main/resources/config.properties.example to ./config.properties and edit it.");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read config.properties", e);
        }
    }

    private Database() { }

    public static String get(String key, String def) { return PROPS.getProperty(key, def); }

    public static int getInt(String key, int def) {
        try { return Integer.parseInt(PROPS.getProperty(key, String.valueOf(def)).trim()); }
        catch (NumberFormatException e) { return def; }
    }

    public static boolean getBool(String key, boolean def) {
        return Boolean.parseBoolean(PROPS.getProperty(key, String.valueOf(def)).trim());
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(get("db.url", ""), get("db.user", ""), get("db.password", ""));
    }
}
