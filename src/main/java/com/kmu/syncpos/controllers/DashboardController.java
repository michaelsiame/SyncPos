// src/main/java/com/kmu/syncpos/controllers/DashboardController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.ProductDTO;
import com.kmu.syncpos.dto.dashboard.DashboardKPIs;
import com.kmu.syncpos.models.Product;
import com.kmu.syncpos.service.DashboardService;
import com.kmu.syncpos.util.ModelMapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    // --- FXML Components ---
    @FXML private Label todaysSalesLabel;
    @FXML private Label todaysTransactionsLabel;
    @FXML private ListView<Product> lowStockListView;
    @FXML private BarChart<String, Number> salesChart;

    // --- Services ---
    private final DashboardService dashboardService = new DashboardService();

    @FXML
    public void initialize() {
        loadKPIs();
        loadLowStockItems();
        loadSalesChart();
    }

    private void loadKPIs() {
        DashboardKPIs kpis = dashboardService.getTodaysKPIs();
        todaysSalesLabel.setText(String.format("$%.2f", kpis.getTodaysSalesTotal()));
        todaysTransactionsLabel.setText(String.valueOf(kpis.getTodaysTransactionCount()));
    }

    private void loadLowStockItems() {
        List<ProductDTO> lowStockDTOs = dashboardService.getLowStockProducts();
        List<Product> lowStockItems = lowStockDTOs.stream()
                .map(ModelMapper::fromDto)
                .collect(Collectors.toList());

        lowStockListView.setItems(FXCollections.observableArrayList(lowStockItems));

        lowStockListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s (Stock: %.1f, Min: %d)",
                            item.getName(),
                            item.getCurrentStock(),
                            (long) item.getMinStockLevel()));
                }
            }
        });
    }

    private void loadSalesChart() {
        Map<LocalDate, Double> salesData = dashboardService.getSalesForLast7Days();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Sales");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");

        for (Map.Entry<LocalDate, Double> entry : salesData.entrySet()) {
            String dateLabel = entry.getKey().format(formatter);
            series.getData().add(new XYChart.Data<>(dateLabel, entry.getValue()));
        }

        salesChart.getData().clear();
        salesChart.getData().add(series);
    }
}