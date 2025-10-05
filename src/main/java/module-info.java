module com.kmu.syncpos {
    // --- JavaFX Modules ---
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // --- Core Java Modules ---
    requires java.sql;

    // --- Third-Party Modules---
    requires com.google.gson;
    requires org.xerial.sqlitejdbc;
    requires okhttp3;
    requires jbcrypt;
    requires static lombok;
    requires java.desktop;

    // --- Opening Packages for Reflection ---
    opens com.kmu.syncpos to javafx.fxml;
    opens com.kmu.syncpos.controllers to javafx.fxml;

    // Combined opens for both modules
    opens com.kmu.syncpos.models to javafx.base, com.google.gson;
    opens com.kmu.syncpos.dto to com.google.gson;

    opens com.kmu.syncpos.models.reports to javafx.base;

    // --- Exporting Packages ---
    exports com.kmu.syncpos;
    exports com.kmu.syncpos.controllers;
}