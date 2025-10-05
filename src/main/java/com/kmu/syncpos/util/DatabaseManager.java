package com.kmu.syncpos.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the connection to and initialization of the local SQLite database.
 * This class implements a singleton pattern to ensure a single point of database access.
 * The schema defined here is "strict" - it uses NOT NULL and DEFAULT constraints
 * to prevent bad data from being stored, which is a primary defense against
 * runtime NullPointerExceptions.
 */
public class DatabaseManager {

    private static final String DB_FOLDER_NAME = ".syncpos";
    private static final String DB_FILE_NAME = "syncpos.db";
    private static final String DB_URL = "jdbc:sqlite:" + System.getProperty("user.home") + File.separator + DB_FOLDER_NAME + File.separator + DB_FILE_NAME;

    private static DatabaseManager instance;

    private DatabaseManager() {
        // Private constructor for singleton
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public void initializeDatabase() {
        // This method creates/verifies the database schema
        File dbFolder = new File(System.getProperty("user.home"), DB_FOLDER_NAME);
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                System.out.println("Database connection established. Creating/verifying tables...");
                // Execute each CREATE TABLE statement individually to avoid issues
                for (String tableSchema : getSchema().split(";")) {
                    if (!tableSchema.trim().isEmpty()) {
                        stmt.executeUpdate(tableSchema.trim() + ";");
                    }
                }
                System.out.println("Schema is up to date.");
            }
            conn.commit();
            System.out.println("Database initialization successful.");

            // Run the cleanup task after ensuring tables exist. This is useful for
            // migrating data from older, less-strict versions of the database.
            performDataCleanup();

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks for NULL values in columns and replaces them with defaults.
     * This is primarily a data migration tool for databases created with an older,
     * less strict schema. For new databases, the NOT NULL constraints in the schema
     * should prevent these issues from occurring in the first place.
     */
    public void performDataCleanup() {
        String[] queries = {
                // Fix NULLs in the 'products' table
                "UPDATE products SET category_id = 0 WHERE category_id IS NULL",
                "UPDATE products SET unit_id = 0 WHERE unit_id IS NULL",
                "UPDATE products SET supplier_id = 0 WHERE supplier_id IS NULL",
                "UPDATE products SET purchase_price = 0.0 WHERE purchase_price IS NULL",
                "UPDATE products SET selling_price = 0.0 WHERE selling_price IS NULL",
                "UPDATE products SET tax_rate = 0.0 WHERE tax_rate IS NULL",
                "UPDATE products SET min_stock_level = 0.0 WHERE min_stock_level IS NULL",
                "UPDATE products SET reorder_quantity = 0.0 WHERE reorder_quantity IS NULL",
                "UPDATE products SET current_stock = 0.0 WHERE current_stock IS NULL",

                // Fix NULLs in the 'categories' table
                "UPDATE categories SET parent_id = 0 WHERE parent_id IS NULL",

                // Fix NULLs in the 'suppliers' table
                "UPDATE suppliers SET credit_limit = 0.0 WHERE credit_limit IS NULL",

                // Fix NULLs in the 'stock_ledger' table
                "UPDATE stock_ledger SET sale_item_id = 0 WHERE sale_item_id IS NULL"
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            System.out.println("Performing data cleanup/migration...");
            conn.setAutoCommit(false);

            for (String query : queries) {
                int rowsAffected = stmt.executeUpdate(query);
                if (rowsAffected > 0) {
                    System.out.println("Cleaned " + rowsAffected + " rows with query: " + query);
                }
            }

            conn.commit();
            System.out.println("Data cleanup/migration completed successfully.");

        } catch (SQLException e) {
            System.err.println("Data cleanup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getSchema() {
        return """
    CREATE TABLE IF NOT EXISTS tenants (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        license_key TEXT UNIQUE NOT NULL,
        owner_email TEXT,
        status TEXT NOT NULL,
        is_synced INTEGER NOT NULL DEFAULT 0,
        expiry_date DATE,
        created_at DATETIME
    );

    CREATE TABLE IF NOT EXISTS settings (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL REFERENCES tenants(uuid),
        setting_key TEXT NOT NULL,
        setting_value TEXT,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0,
        UNIQUE(tenant_id, setting_key)
    );

    CREATE TABLE IF NOT EXISTS users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL REFERENCES tenants(uuid),
        username TEXT NOT NULL UNIQUE,
        firstname TEXT NOT NULL,
        lastname TEXT NOT NULL,
        password_hash TEXT NOT NULL,
        email TEXT,
        phone TEXT,
        role TEXT NOT NULL,
        is_active INTEGER NOT NULL DEFAULT 1,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS suppliers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL REFERENCES tenants(uuid),
        name TEXT NOT NULL,
        contact_person TEXT,
        email TEXT,
        phone TEXT,
        address TEXT,
        payment_terms TEXT,
        credit_limit REAL NOT NULL DEFAULT 0.0,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS customers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL REFERENCES tenants(uuid),
        name TEXT NOT NULL,
        email TEXT,
        phone TEXT,
        address TEXT,
        loyalty_points INTEGER NOT NULL DEFAULT 0,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS categories (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL REFERENCES tenants(uuid),
        name TEXT NOT NULL,
        description TEXT,
        parent_id INTEGER, -- Nullable by design for top-level categories
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS units (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL REFERENCES tenants(uuid),
        name TEXT NOT NULL,
        abbreviation TEXT NOT NULL,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );

    -- This is the most critical table to fortify.
    -- All numeric fields that caused NullPointerExceptions are now NOT NULL.
    CREATE TABLE IF NOT EXISTS products (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL,
        sku TEXT,
        barcode TEXT,
        name TEXT NOT NULL,
        description TEXT,
        product_type TEXT NOT NULL DEFAULT 'PHYSICAL',
        -- Foreign keys are left nullable here to prevent crashing if a product is created
        -- without a category. The DAO/cleanup logic should handle setting a default (e.g., 0).
        category_id INTEGER,
        unit_id INTEGER,
        supplier_id INTEGER,
        -- **CRITICAL FIX**: All numeric values are now non-nullable.
        purchase_price REAL NOT NULL DEFAULT 0.0,
        selling_price REAL NOT NULL DEFAULT 0.0,
        tax_rate REAL NOT NULL DEFAULT 0.0,
        min_stock_level REAL NOT NULL DEFAULT 0.0,
        reorder_quantity REAL NOT NULL DEFAULT 0.0,
        current_stock REAL NOT NULL DEFAULT 0.0, -- Changed to REAL for consistency
        is_active INTEGER NOT NULL DEFAULT 1,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS product_suppliers (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL,
        product_id INTEGER NOT NULL REFERENCES products(id),
        supplier_id INTEGER NOT NULL REFERENCES suppliers(id),
        supplier_product_code TEXT,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0,
        UNIQUE(tenant_id, product_id, supplier_id)
    );

    CREATE TABLE IF NOT EXISTS sales (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL REFERENCES tenants(uuid),
        type TEXT NOT NULL,
        user_id INTEGER NOT NULL,
        customer_id INTEGER, -- Nullable by design (e.g., for purchases)
        supplier_id INTEGER, -- Nullable by design (e.g., for sales)
        subtotal REAL NOT NULL,
        payment_method TEXT,
        tax REAL NOT NULL DEFAULT 0.0,
        discount REAL NOT NULL DEFAULT 0.0,
        total REAL NOT NULL,
        payment_status TEXT NOT NULL DEFAULT 'pending',
        notes TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS sale_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL,
        sale_id INTEGER NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
        product_id INTEGER NOT NULL,
        supplier_product_code TEXT,
        quantity REAL NOT NULL,
        unit_price REAL NOT NULL,
        cost_at_sale REAL NOT NULL,
        tax_rate REAL NOT NULL,
        discount REAL NOT NULL DEFAULT 0.0,
        total REAL NOT NULL,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS payments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL REFERENCES tenants(uuid),
        sale_id INTEGER NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
        amount REAL NOT NULL,
        payment_method TEXT NOT NULL,
        reference TEXT,
        user_id INTEGER NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS stock_ledger (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        uuid TEXT UNIQUE NOT NULL,
        tenant_id TEXT NOT NULL REFERENCES tenants(uuid),
        product_id INTEGER NOT NULL,
        quantity_delta REAL NOT NULL,
        reason TEXT NOT NULL,
        sale_item_id INTEGER, -- Nullable by design (e.g., for stock takes)
        user_id INTEGER NOT NULL,
        notes TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        last_updated_at DATETIME,
        is_synced INTEGER NOT NULL DEFAULT 0,
        is_deleted INTEGER NOT NULL DEFAULT 0
    );
    """;
    }

    public String getDatabasePath() {
        String url = DB_URL;
        if (url != null && url.startsWith("jdbc:sqlite:")) {
            return url.substring("jdbc:sqlite:".length());
        }
        return "Unknown or in-memory database";
    }
}