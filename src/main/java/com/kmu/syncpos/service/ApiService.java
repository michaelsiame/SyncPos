// src/main/java/com/kmu/syncpos/service/ApiService.java
package com.kmu.syncpos.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kmu.syncpos.util.LocalDateAdapter;
import com.kmu.syncpos.util.OffsetDateTimeAdapter;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import com.kmu.syncpos.dto.*;

/**
 * A service for communicating with the remote Supabase API.
 * This class is responsible for all HTTP requests and serialization/deserialization.
 */
public final class ApiService {

    // --- Constants ---
    private static final String SUPABASE_URL = "https://tsbanhacgiqxilvfydpf.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRzYmFuaGFjZ2lxeGlsdmZ5ZHBmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTQxMjAyMTYsImV4cCI6MjA2OTY5NjIxNn0.DlaXJASotyIqFs4bEIk6mbmfyMgsVGllNjOlusSc0Vw";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // --- Shared Components ---
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    private <D> boolean post(String table, D dto) {
        String url = SUPABASE_URL + "/rest/v1/" + table;
        RequestBody body = RequestBody.create(gson.toJson(dto), JSON);
        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates") // Use merge-duplicates for upsert logic
                .post(body)
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                logError("POST", url, resp);
                return false;
            }
            return true;
        } catch (IOException e) {
            logException("POST", url, e);
            return false;
        }
    }

    public TenantDTO getTenantByKey(String licenseKey) {
        String url = SUPABASE_URL + "/rest/v1/tenants?license_key=eq." + licenseKey + "&limit=1";
        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .addHeader("Accept", "application/vnd.pgrst.object+json")
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                logError("GET", url, resp);
                return null;
            }
            return gson.fromJson(resp.body().string(), TenantDTO.class);
        } catch (IOException e) {
            logException("GET", url, e);
            return null;
        }
    }

    private <T> List<T> getAllForTenant(String tableName, String tenantId, Type typeOfT) {
        String url = SUPABASE_URL + "/rest/v1/" + tableName + "?tenant_id=eq." + tenantId;
        Request req = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) {
                logError("GET " + tableName, url, resp);
                return Collections.emptyList();
            }
            return gson.fromJson(resp.body().string(), typeOfT);
        } catch (IOException e) {
            logException("GET " + tableName, url, e);
            return Collections.emptyList();
        }
    }

    // --- Entity-Specific POST Methods ---

    public boolean postSettings(SettingsDTO dto) { return post("settings", dto); }
    public boolean postUser(UserDTO dto) { return post("users", dto); }
    public boolean postSupplier(SupplierDTO dto) { return post("suppliers", dto); }
    public boolean postCustomer(CustomerDTO dto) { return post("customers", dto); }
    public boolean postCategory(CategoryDTO dto) { return post("categories", dto); }
    public boolean postUnit(UnitDTO dto) { return post("units", dto); }
    public boolean postProduct(ProductDTO dto) { return post("products", dto); }
    public boolean postSale(SaleDTO dto) { return post("sales", dto); }
    public boolean postSaleItem(SaleItemDTO dto) { return post("sale_items", dto); }
    public boolean postPayment(PaymentDTO dto) { return post("payments", dto); }

    // Corrected table name to match our schema
    public boolean postStockLedger(StockLedgerDTO dto) {
        return post("stock_ledger", dto);
    }

    // --- CHANGE 1: ADDED NEW POST METHOD ---
    public boolean postProductSupplier(ProductSupplierDTO dto) {
        return post("product_suppliers", dto);
    }

    // --- Entity-Specific GET ALL Methods ---

    public List<CategoryDTO> getAllCategories(String tenantId) {
        return getAllForTenant("categories", tenantId, new TypeToken<List<CategoryDTO>>() {}.getType());
    }
    public List<CustomerDTO> getAllCustomers(String tenantId) {
        return getAllForTenant("customers", tenantId, new TypeToken<List<CustomerDTO>>() {}.getType());
    }
    public List<ProductDTO> getAllProducts(String tenantId) {
        return getAllForTenant("products", tenantId, new TypeToken<List<ProductDTO>>() {}.getType());
    }
    public List<SettingsDTO> getAllSettings(String tenantId) {
        return getAllForTenant("settings", tenantId, new TypeToken<List<SettingsDTO>>() {}.getType());
    }
    public List<SupplierDTO> getAllSuppliers(String tenantId) {
        return getAllForTenant("suppliers", tenantId, new TypeToken<List<SupplierDTO>>() {}.getType());
    }
    public List<UnitDTO> getAllUnits(String tenantId) {
        return getAllForTenant("units", tenantId, new TypeToken<List<UnitDTO>>() {}.getType());
    }
    public List<UserDTO> getAllUsers(String tenantId) {
        return getAllForTenant("users", tenantId, new TypeToken<List<UserDTO>>() {}.getType());
    }
    public List<SaleDTO> getAllSales(String tenantId) {
        return getAllForTenant("sales", tenantId, new TypeToken<List<SaleDTO>>() {}.getType());
    }
    public List<SaleItemDTO> getAllSaleItems(String tenantId) {
        return getAllForTenant("sale_items", tenantId, new TypeToken<List<SaleItemDTO>>() {}.getType());
    }
    public List<PaymentDTO> getAllPayments(String tenantId) {
        return getAllForTenant("payments", tenantId, new TypeToken<List<PaymentDTO>>() {}.getType());
    }

    // --- CHANGE 2: CORRECTED TABLE NAME ---
    public List<StockLedgerDTO> getAllStockLedgerEntries(String tenantId) {
        return getAllForTenant("stock_ledger", tenantId, new TypeToken<List<StockLedgerDTO>>() {}.getType());
    }

    // --- CHANGE 3: ADDED NEW GET ALL METHOD ---
    public List<ProductSupplierDTO> getAllProductSuppliers(String tenantId) {
        return getAllForTenant("product_suppliers", tenantId, new TypeToken<List<ProductSupplierDTO>>() {}.getType());
    }

    // --- Logging Helper Methods ---

    private void logError(String method, String url, Response resp) {
        try {
            System.err.printf("%s request to %s failed (%d): %s%n", method, url, resp.code(), resp.body().string());
        } catch (IOException | NullPointerException e) {
            System.err.printf("%s request to %s failed (%d) with an additional error while reading the response body.%n", method, url, resp.code());
        }
    }

    private void logException(String method, String url, Exception e) {
        System.err.printf("%s request to %s threw an exception: %s%n", method, url, e.getMessage());
    }
}