// src/main/java/com/kmu/syncpos/controllers/ReportsController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.ProductDTO;
import com.kmu.syncpos.models.Product;
import com.kmu.syncpos.models.reports.ProductPerformanceRecord;
import com.kmu.syncpos.models.reports.SalesReportRecord;
import com.kmu.syncpos.service.ReportService;
import com.kmu.syncpos.util.ModelMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ReportsController {

    // --- FXML Components ---
    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button generateReportButton;
    @FXML private StackPane reportContentPane;

    // --- Services ---
    private final ReportService reportService = new ReportService();

    @FXML
    public void initialize() {
        reportTypeComboBox.getItems().addAll(
                "Sales Detail Report",
                "Product Performance Report",
                "Inventory Value Report"
        );
        startDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        endDatePicker.setValue(LocalDate.now());
    }

    @FXML
    private void handleGenerateReport() {
        String reportType = reportTypeComboBox.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (reportType == null || startDate == null || endDate == null) {
            showAlert(Alert.AlertType.WARNING, "Input Required", "Please select a report type and a date range.");
            return;
        }

        for (Node node : reportContentPane.getChildren()) {
            node.setVisible(false);
        }

        switch (reportType) {
            case "Sales Detail Report" -> generateSalesDetailReport(startDate, endDate);
            case "Product Performance Report" -> generateProductPerformanceReport(startDate, endDate);
            case "Inventory Value Report" -> generateInventoryValueReport();
        }
    }

    private void generateSalesDetailReport(LocalDate start, LocalDate end) {
        List<SalesReportRecord> data = reportService.getSalesDetailReport(start, end);
        TableView<SalesReportRecord> tableView = new TableView<>(FXCollections.observableArrayList(data));

        TableColumn<SalesReportRecord, Long> idCol = new TableColumn<>("Trans. ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("transactionId"));

        TableColumn<SalesReportRecord, LocalDateTime> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        TableColumn<SalesReportRecord, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<SalesReportRecord, String> cashierCol = new TableColumn<>("Cashier");
        cashierCol.setCellValueFactory(new PropertyValueFactory<>("cashierName"));

        TableColumn<SalesReportRecord, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        tableView.getColumns().setAll(idCol, dateCol, customerCol, cashierCol, totalCol);
        reportContentPane.getChildren().setAll(tableView);
        tableView.setVisible(true);
    }

    private void generateProductPerformanceReport(LocalDate start, LocalDate end) {
        List<ProductPerformanceRecord> data = reportService.getProductPerformanceReport(start, end);
        TableView<ProductPerformanceRecord> tableView = new TableView<>(FXCollections.observableArrayList(data));

        TableColumn<ProductPerformanceRecord, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        nameCol.setPrefWidth(250);

        TableColumn<ProductPerformanceRecord, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));

        TableColumn<ProductPerformanceRecord, Double> qtyCol = new TableColumn<>("Qty Sold");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));

        TableColumn<ProductPerformanceRecord, Double> revenueCol = new TableColumn<>("Total Revenue");
        revenueCol.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
        revenueCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        tableView.getColumns().setAll(nameCol, skuCol, qtyCol, revenueCol);
        reportContentPane.getChildren().setAll(tableView);
        tableView.setVisible(true);
    }

    private void generateInventoryValueReport() {
        List<ProductDTO> productDTOs = reportService.getInventoryValueReport();
        List<Product> data = productDTOs.stream().map(ModelMapper::fromDto).collect(Collectors.toList());
        TableView<Product> tableView = new TableView<>(FXCollections.observableArrayList(data));

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);

        TableColumn<Product, Double> stockCol = new TableColumn<>("Current Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("currentStock"));

        TableColumn<Product, Double> costCol = new TableColumn<>("Purchase Price");
        costCol.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        costCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<Product, Double> valueCol = new TableColumn<>("Inventory Value");
        valueCol.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            double value = p.getCurrentStock() * p.getPurchasePrice();
            return new javafx.beans.property.SimpleDoubleProperty(value).asObject();
        });
        valueCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        tableView.getColumns().setAll(nameCol, stockCol, costCol, valueCol);
        reportContentPane.getChildren().setAll(tableView);
        tableView.setVisible(true);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}