// src/main/java/com/kmu/syncpos/service/ReceiptService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.dto.SaleDTO;
import com.kmu.syncpos.dto.SaleItemDTO;
import com.kmu.syncpos.models.Product;

import javax.print.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Handles printing receipts and test pages.
 * It gracefully handles cases where no printer is configured or found.
 */
public class ReceiptService {

    private final SettingsService settingsService;

    public ReceiptService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Prints a full sales receipt.
     *
     * @param sale         The Sale DTO containing total amounts.
     * @param items        The list of items in the sale.
     * @param products     The list of full product models (for names).
     * @param customerName The name of the customer.
     * @throws PrintException if there is an error sending the job to the printer.
     */
    public void printReceipt(SaleDTO sale, List<SaleItemDTO> items, List<Product> products, String customerName) throws PrintException {
        PrintService printer = findPrinter();
        if (printer == null) {
            System.err.println("Receipt printing skipped: No printer configured or found.");
            return; // Don't throw an error, just skip printing.
        }

        String receiptContent = buildReceiptContent(sale, items, products, customerName);
        sendToPrinter(printer, receiptContent);
    }

    /**
     * Prints a simple test page to the configured printer.
     * @throws PrintException if there is an error sending the job to the printer.
     */
    public void printTestPage() throws PrintException {
        PrintService printer = findPrinter();
        if (printer == null) {
            throw new PrintException("Cannot print test page: No printer configured or found.");
        }

        String testContent = buildTestContent();
        sendToPrinter(printer, testContent);
    }

    private String buildReceiptContent(SaleDTO sale, List<SaleItemDTO> items, List<Product> products, String customerName) {
        Map<String, String> companySettings = settingsService.getAllSettingsAsMap();
        String currency = companySettings.getOrDefault("currencySymbol", "$");

        StringBuilder sb = new StringBuilder();
        // --- Receipt Header ---
        sb.append(center(companySettings.getOrDefault("companyName", "My POS Store"))).append("\n");
        sb.append(center(companySettings.getOrDefault("companyAddress", "123 Main St"))).append("\n");
        sb.append(center(companySettings.getOrDefault("companyPhone", "555-1234"))).append("\n");
        sb.append("\n");
        sb.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        sb.append("Customer: ").append(customerName).append("\n");
        sb.append("--------------------------------\n");

        // --- Items ---
        sb.append(String.format("%-16s %4s %8s\n", "Item", "Qty", "Total"));
        for (SaleItemDTO item : items) {
            String productName = products.stream()
                    .filter(p -> p.getId() == item.getProductId())
                    .map(Product::getName)
                    .findFirst()
                    .orElse("Unknown Item");
            if (productName.length() > 16) {
                productName = productName.substring(0, 15) + ".";
            }
            sb.append(String.format("%-16s %4.0f %8.2f\n",
                    productName,
                    item.getQuantity(),
                    item.getTotal()));
        }
        sb.append("--------------------------------\n");

        // --- Totals ---
        sb.append(String.format("%21s %9.2f\n", "Subtotal:", sale.getSubtotal()));
        sb.append(String.format("%21s %9.2f\n", "Tax:", sale.getTax()));
        sb.append(String.format("%21s %9.2f\n", "Discount:", -sale.getDiscount())); // show as positive
        sb.append(String.format("%21s %s%8.2f\n", "TOTAL:", currency, sale.getTotal()));
        sb.append("\n");

        // --- Footer ---
        sb.append(center(companySettings.getOrDefault("receiptFooter", "Thank you!"))).append("\n");
        sb.append("\n\n\n");

        return sb.toString();
    }

    private String buildTestContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Printer Test ---\n");
        sb.append("If you can read this, the\n");
        sb.append("printer connection is working.\n");
        sb.append("--------------------\n");
        sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        sb.append("\n\n\n");
        return sb.toString();
    }


    private void sendToPrinter(PrintService printer, String text) throws PrintException {
        DocPrintJob job = printer.createPrintJob();
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        // Many receipt printers require a "cut" command at the end.
        // This is a common ESC/POS command.
        byte[] cutCommand = {0x1D, 0x56, 0x41, 0x10};
        byte[] combined = new byte[bytes.length + cutCommand.length];
        System.arraycopy(bytes, 0, combined, 0, bytes.length);
        System.arraycopy(cutCommand, 0, combined, bytes.length, cutCommand.length);

        Doc doc = new SimpleDoc(combined, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        job.print(doc, null);
    }

    private PrintService findPrinter() {
        String printerName = settingsService.getSetting("printerName",null);
        if (printerName == null || printerName.trim().isEmpty()) {
            return null; // No printer has been configured
        }

        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        return Arrays.stream(printServices)
                .filter(s -> s.getName().equalsIgnoreCase(printerName))
                .findFirst()
                .orElse(null); // The configured printer was not found
    }

    private String center(String text) {
        int width = 32; // Standard receipt paper width in characters
        if (text.length() >= width) {
            return text;
        }
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }
}