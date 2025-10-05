// src/main/java/com/kmu/syncpos/service/CashDrawerService.java
package com.kmu.syncpos.service;

import javax.print.*;
import java.io.IOException;
import java.util.Arrays;

/**
 * Handles opening the cash drawer connected to a receipt printer.
 */
public class CashDrawerService {

    private final SettingsService settingsService;

    public CashDrawerService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Sends the command to open the cash drawer if it's enabled in settings.
     * @throws IOException if the drawer is enabled but the printer is not found or fails to respond.
     */
    public void openDrawer() throws IOException {
        boolean enabled = Boolean.parseBoolean(settingsService.getSetting("cashDrawerEnabled",null));
        if (!enabled) {
            System.out.println("Cash drawer open skipped: feature not enabled.");
            return;
        }

        PrintService printer = findPrinter();
        if (printer == null) {
            throw new IOException("Cash drawer enabled, but configured printer is not found.");
        }

        // Standard ESC/POS command to pulse pin 2 of the RJ11/RJ12 port
        // Command: ESC p m t1 t2
        // m=0 (pin 2), t1=25 (pulse on time), t2=250 (pulse off time)
        byte[] openDrawerCommand = {27, 112, 0, 25, (byte) 250};

        try {
            DocPrintJob job = printer.createPrintJob();
            Doc doc = new SimpleDoc(openDrawerCommand, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, null);
        } catch (PrintException e) {
            throw new IOException("Failed to send open command to cash drawer via printer.", e);
        }
    }

    private PrintService findPrinter() {
        String printerName = settingsService.getSetting("printerName",null);
        if (printerName == null || printerName.trim().isEmpty()) {
            return null;
        }
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        return Arrays.stream(printServices)
                .filter(s -> s.getName().equalsIgnoreCase(printerName))
                .findFirst()
                .orElse(null);
    }
}