package com.kmu.syncpos.util;

import com.kmu.syncpos.dto.*;
import com.kmu.syncpos.models.*;

import java.sql.Timestamp; // A common source of nulls from DAOs
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

/**
 * A final utility class for converting between server-facing Data Transfer Objects (DTOs)
 * and UI-layer Models. This class acts as a crucial anti-corruption layer, decoupling
 * the data persistence/transfer layer from the presentation layer.
 *
 * All mapping methods are null-safe: if a null object is passed in, null is returned.
 */
public final class ModelMapper {

    private ModelMapper() { /* Private constructor to prevent instantiation */ }

    /* --------------------------------------------------
       SHARED CONVERSION HELPERS
       -------------------------------------------------- */

    /**
     * Safely converts an OffsetDateTime (used in DTOs, often with timezone) to a
     * LocalDateTime (used in UI Models).
     *
     * @param odt The OffsetDateTime to convert.
     * @return The corresponding LocalDateTime, or null if the input was null.
     *         This prevents NullPointerExceptions when a DTO has a null timestamp.
     */
    private static LocalDateTime toLdt(OffsetDateTime odt) {
        return odt == null ? null : odt.toLocalDateTime();
    }

    /**
     * Safely converts a LocalDateTime (used in UI Models) to an OffsetDateTime
     * (used in DTOs), assuming UTC.
     *
     * @param ldt The LocalDateTime to convert.
     * @return The corresponding OffsetDateTime at UTC, or null if the input was null.
     */
    private static OffsetDateTime toOdt(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
    }

    /* --------------------------------------------------
       USER
       -------------------------------------------------- */

    /**
     * Converts a UserDTO to a User model. Returns null if the dto is null.
     */
    public static User fromDto(UserDTO dto) {
        if (dto == null) return null;
        User u = new User();
        u.setId(dto.getId());
        u.setUuid(dto.getUuid());
        u.setTenantId(dto.getTenantId());
        u.setUsername(dto.getUsername());
        u.setFirstname(dto.getFirstname());
        u.setLastname(dto.getLastname());
        u.setPasswordHash(dto.getPasswordHash());
        u.setEmail(dto.getEmail());
        u.setPhone(dto.getPhone());
        u.setRole(dto.getRole());
        u.setIsActive(dto.isActive());
        u.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        u.setIsSynced(dto.getIsSynced());
        u.setIsDeleted(dto.isDeleted());
        return u;
    }

    /**
     * Converts a User model to a UserDTO. Returns null if the model is null.
     */
    public static UserDTO toDto(User u) {
        if (u == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(u.getId());
        dto.setUuid(u.getUuid());
        dto.setTenantId(u.getTenantId());
        dto.setUsername(u.getUsername());
        dto.setFirstname(u.getFirstname());
        dto.setLastname(u.getLastname());
        dto.setPasswordHash(u.getPasswordHash());
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        dto.setRole(u.getRole());
        dto.setActive(u.getIsActive());
        dto.setLastUpdatedAt(toOdt(u.getLastUpdatedAt()));
        dto.setDeleted(u.getIsDeleted());
        dto.setIsSynced(u.getIsSynced());
        return dto;
    }

    /* --------------------------------------------------
       SUPPLIER
       -------------------------------------------------- */

    /**
     * Converts a SupplierDTO to a Supplier model. Returns null if the dto is null.
     */
    public static Supplier fromDto(SupplierDTO dto) {
        if (dto == null) return null;
        Supplier s = new Supplier();
        s.setId(dto.getId());
        s.setUuid(dto.getUuid());
        s.setTenantId(dto.getTenantId());
        s.setName(dto.getName());
        s.setContactPerson(dto.getContactPerson());
        s.setEmail(dto.getEmail());
        s.setPhone(dto.getPhone());
        s.setAddress(dto.getAddress());
        s.setPaymentTerms(dto.getPaymentTerms());
        s.setCreditLimit(dto.getCreditLimit());
        s.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        s.setIsSynced(dto.getIsSynced());
        s.setIsDeleted(dto.isDeleted());
        return s;
    }

    /**
     * Converts a Supplier model to a SupplierDTO. Returns null if the model is null.
     */
    public static SupplierDTO toDto(Supplier s) {
        if (s == null) return null;
        SupplierDTO dto = new SupplierDTO();
        dto.setId(s.getId());
        dto.setUuid(s.getUuid());
        dto.setTenantId(s.getTenantId());
        dto.setName(s.getName());
        dto.setContactPerson(s.getContactPerson());
        dto.setEmail(s.getEmail());
        dto.setPhone(s.getPhone());
        dto.setAddress(s.getAddress());
        dto.setPaymentTerms(s.getPaymentTerms());
        dto.setCreditLimit(s.getCreditLimit());
        dto.setLastUpdatedAt(toOdt(s.getLastUpdatedAt()));
        dto.setIsSynced(s.getIsSynced());
        dto.setDeleted(s.getIsDeleted());
        return dto;
    }

    /* --------------------------------------------------
       CUSTOMER
       -------------------------------------------------- */

    /**
     * Converts a CustomerDTO to a Customer model. Returns null if the dto is null.
     */
    public static Customer fromDto(CustomerDTO dto) {
        if (dto == null) return null;
        Customer c = new Customer();
        c.setId(dto.getId());
        c.setUuid(dto.getUuid());
        c.setTenantId(dto.getTenantId());
        c.setName(dto.getName());
        c.setEmail(dto.getEmail());
        c.setPhone(dto.getPhone());
        c.setAddress(dto.getAddress());
        c.setLoyaltyPoints(dto.getLoyaltyPoints());
        c.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        c.setIsDeleted(dto.isDeleted());
        c.setIsSynced(dto.getIsSynced());
        return c;
    }

    /**
     * Converts a Customer model to a CustomerDTO. Returns null if the model is null.
     */
    public static CustomerDTO toDto(Customer c) {
        if (c == null) return null;
        CustomerDTO dto = new CustomerDTO();
        dto.setId(c.getId());
        dto.setUuid(c.getUuid());
        dto.setTenantId(c.getTenantId());
        dto.setName(c.getName());
        dto.setEmail(c.getEmail());
        dto.setPhone(c.getPhone());
        dto.setAddress(c.getAddress());
        dto.setLoyaltyPoints(c.getLoyaltyPoints());
        dto.setLastUpdatedAt(toOdt(c.getLastUpdatedAt()));
        dto.setDeleted(c.getIsDeleted());
        dto.setIsSynced(c.getIsSynced());
        return dto;
    }

    /* --------------------------------------------------
       PRODUCT
       -------------------------------------------------- */

    /**
     * Converts a ProductDTO to a Product model. Returns null if the dto is null.
     */
    public static Product fromDto(ProductDTO dto) {
        if (dto == null) return null;
        Product p = new Product();
        p.setId(dto.getId());
        p.setUuid(dto.getUuid());
        p.setTenantId(dto.getTenantId());
        p.setSku(dto.getSku());
        p.setBarcode(dto.getBarcode());
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setProductType(dto.getProductType());
        p.setCategoryId(dto.getCategoryId()!= null ? dto.getCategoryId() : null);
        p.setUnitId(dto.getUnitId() != null ? dto.getUnitId() : null);
        p.setSupplierId(dto.getSupplierId() != null ? dto.getSupplierId() : null);
        p.setCategoryUuid(dto.getCategoryUuid());
        p.setUnitUuid(dto.getUnitUuid());
        p.setSupplierUuid(dto.getSupplierUuid());
        p.setPurchasePrice(dto.getPurchasePrice() != null ? dto.getPurchasePrice() : 0.0);
        p.setSellingPrice(dto.getSellingPrice() != null ? dto.getSellingPrice() : 0.0);
        p.setTaxRate(dto.getTaxRate() != null ? dto.getTaxRate() : 0.0);
        p.setMinStockLevel(dto.getMinStockLevel() != null ? dto.getMinStockLevel() : 0.0);
        p.setReorderQuantity(dto.getReorderQuantity() != null ? dto.getReorderQuantity() : 0.0);
        p.setIsActive(dto.isActive());
        p.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        p.setIsSynced(dto.getIsSynced());
        p.setIsDeleted(dto.isDeleted());
        p.setCurrentStock(dto.getCurrentStock() != null ? dto.getCurrentStock() : 0.0);
        return p;
    }

    /**
     * Converts a Product model to a ProductDTO. Returns null if the model is null.
     */
    public static ProductDTO toDto(Product p) {
        if (p == null) return null;
        ProductDTO dto = new ProductDTO();
        dto.setId(p.getId());
        dto.setUuid(p.getUuid());
        dto.setTenantId(p.getTenantId());
        dto.setSku(p.getSku());
        dto.setBarcode(p.getBarcode());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setProductType(p.getProductType());
        dto.setCategoryId(p.getCategoryId());
        dto.setUnitId(p.getUnitId());
        dto.setSupplierId(p.getSupplierId());
        dto.setCategoryUuid(p.getCategoryUuid());
        dto.setUnitUuid(p.getUnitUuid());
        dto.setSupplierUuid(p.getSupplierUuid());
        dto.setPurchasePrice(p.getPurchasePrice());
        dto.setSellingPrice(p.getSellingPrice());
        dto.setTaxRate(p.getTaxRate());
        dto.setMinStockLevel(p.getMinStockLevel());
        dto.setReorderQuantity(p.getReorderQuantity());
        dto.setActive(p.getIsActive());
        dto.setLastUpdatedAt(toOdt(p.getLastUpdatedAt()));
        dto.setIsSynced(p.getIsSynced());
        dto.setDeleted(p.getIsDeleted());
        dto.setCurrentStock(p.getCurrentStock());
        return dto;
    }

    /* --------------------------------------------------
       SALE
       -------------------------------------------------- */

    /**
     * Converts a SaleDTO to a Sale model. Items are not mapped. Returns null if the dto is null.
     */
    public static Sale fromDto(SaleDTO dto) {
        if (dto == null) return null;
        Sale s = new Sale();
        s.setId(dto.getId());
        s.setUuid(dto.getUuid());
        s.setTenantId(dto.getTenantId());
        s.setType(dto.getType());
        s.setUserId(dto.getUserId());
        s.setCustomerId(dto.getCustomerId());
        s.setSupplierId(dto.getSupplierId());
        s.setCustomerUuid(dto.getCustomerUuid());
        s.setSupplierUuid(dto.getSupplierUuid());
        s.setSubtotal(dto.getSubtotal());
        s.setTax(dto.getTax());
        s.setDiscount(dto.getDiscount());
        s.setTotal(dto.getTotal());
        s.setPaymentMethod(dto.getPaymentMethod());
        s.setPaymentStatus(dto.getPaymentStatus());
        s.setNotes(dto.getNotes());
        s.setCreatedAt(toLdt(dto.getCreatedAt()));
        s.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        s.setIsSynced(dto.getIsSynced());
        s.setIsDeleted(dto.isDeleted());
        return s;
    }

    /**
     * Converts a Sale model to a SaleDTO. Items list is initialized but not populated.
     * Returns null if the model is null.
     */
    public static SaleDTO toDto(Sale s) {
        if (s == null) return null;
        SaleDTO dto = new SaleDTO();
        dto.setId(s.getId());
        dto.setUuid(s.getUuid());
        dto.setTenantId(s.getTenantId());
        dto.setType(s.getType());
        dto.setUserId(s.getUserId());
        dto.setCustomerId(s.getCustomerId());
        dto.setSupplierId(s.getSupplierId());
        dto.setCustomerUuid(s.getCustomerUuid());
        dto.setSupplierUuid(s.getSupplierUuid());
        dto.setSubtotal(s.getSubtotal());
        dto.setTax(s.getTax());
        dto.setDiscount(s.getDiscount());
        dto.setTotal(s.getTotal());
        dto.setPaymentMethod(s.getPaymentMethod());
        dto.setPaymentStatus(s.getPaymentStatus());
        dto.setNotes(s.getNotes());
        dto.setCreatedAt(toOdt(s.getCreatedAt()));
        dto.setLastUpdatedAt(toOdt(s.getLastUpdatedAt()));
        dto.setIsSynced(s.getIsSynced());
        dto.setDeleted(s.getIsDeleted());
        // Items are handled by the calling service to ensure transactional integrity
        dto.setItems(new ArrayList<>());
        return dto;
    }

    /* --------------------------------------------------
       SALE ITEM
       -------------------------------------------------- */

    /**
     * Converts a SaleItemDTO to a SaleItem model. Returns null if the dto is null.
     */
    public static SaleItem fromDto(SaleItemDTO dto) {
        if (dto == null) return null;
        SaleItem si = new SaleItem();
        si.setId(dto.getId());
        si.setUuid(dto.getUuid());
        si.setTenantId(dto.getTenantId());
        si.setSaleId(dto.getSaleId());
        si.setProductId(dto.getProductId());
        si.setSaleUuid(dto.getSaleUuid());
        si.setProductUuid(dto.getProductUuid());
        si.setSupplierProductCode(dto.getSupplierProductCode());
        si.setQuantity(dto.getQuantity());
        si.setUnitPrice(dto.getUnitPrice());
        si.setCostAtSale(dto.getCostAtSale());
        si.setTaxRate(dto.getTaxRate());
        si.setDiscount(dto.getDiscount());
        si.setTotal(dto.getTotal());
        si.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        si.setIsSynced(dto.getIsSynced());
        si.setIsDeleted(dto.isDeleted());
        return si;
    }

    /**
     * Converts a SaleItem model to a SaleItemDTO. Returns null if the model is null.
     */
    public static SaleItemDTO toDto(SaleItem si) {
        if (si == null) return null;
        SaleItemDTO dto = new SaleItemDTO();
        dto.setId(si.getId());
        dto.setUuid(si.getUuid());
        dto.setTenantId(si.getTenantId());
        dto.setSaleId(si.getSaleId());
        dto.setProductId(si.getProductId());
        dto.setSaleUuid(si.getSaleUuid());
        dto.setProductUuid(si.getProductUuid());
        dto.setSupplierProductCode(si.getSupplierProductCode());
        dto.setQuantity(si.getQuantity());
        dto.setUnitPrice(si.getUnitPrice());
        dto.setCostAtSale(si.getCostAtSale());
        dto.setTaxRate(si.getTaxRate());
        dto.setDiscount(si.getDiscount());
        dto.setTotal(si.getTotal());
        dto.setLastUpdatedAt(toOdt(si.getLastUpdatedAt()));
        dto.setIsSynced(si.getIsSynced());
        dto.setDeleted(si.getIsDeleted());
        return dto;
    }

    /* --------------------------------------------------
       PAYMENT
       -------------------------------------------------- */

    /**
     * Converts a PaymentDTO to a Payment model. Returns null if the dto is null.
     */
    public static Payment fromDto(PaymentDTO dto) {
        if (dto == null) return null;
        Payment p = new Payment();
        p.setId(dto.getId());
        p.setUuid(dto.getUuid());
        p.setTenantId(dto.getTenantId());
        p.setSaleId(dto.getSaleId());
        p.setSaleUuid(dto.getSaleUuid());
        p.setAmount(dto.getAmount());
        p.setPaymentMethod(dto.getPaymentMethod());
        p.setReference(dto.getReference());
        p.setUserId(dto.getUserId());
        p.setCreatedAt(toLdt(dto.getCreatedAt()));
        p.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        p.setIsSynced(dto.getIsSynced());
        p.setIsDeleted(dto.isDeleted());
        return p;
    }

    /**
     * Converts a Payment model to a PaymentDTO. Returns null if the model is null.
     */
    public static PaymentDTO toDto(Payment p) {
        if (p == null) return null;
        PaymentDTO dto = new PaymentDTO();
        dto.setId(p.getId());
        dto.setUuid(p.getUuid());
        dto.setTenantId(p.getTenantId());
        dto.setSaleId(p.getSaleId());
        dto.setSaleUuid(p.getSaleUuid());
        dto.setAmount(p.getAmount());
        dto.setPaymentMethod(p.getPaymentMethod());
        dto.setReference(p.getReference());
        dto.setUserId(p.getUserId());
        dto.setCreatedAt(toOdt(p.getCreatedAt()));
        dto.setLastUpdatedAt(toOdt(p.getLastUpdatedAt()));
        dto.setIsSynced(p.getIsSynced());
        dto.setDeleted(p.getIsDeleted());
        return dto;
    }

    /* --------------------------------------------------
       STOCK LEDGER
       -------------------------------------------------- */

    /**
     * Converts a StockLedgerDTO to a StockLedger model. Returns null if the dto is null.
     */
    public static StockLedger fromDto(StockLedgerDTO dto) {
        if (dto == null) return null;
        StockLedger sl = new StockLedger();
        sl.setId(dto.getId());
        sl.setUuid(dto.getUuid());
        sl.setTenantId(dto.getTenantId());
        sl.setProductId(dto.getProductId());
        sl.setProductUuid(dto.getProductUuid());
        sl.setQuantityDelta(dto.getQuantityDelta());
        sl.setReason(dto.getReason());
        sl.setsaleItemId(dto.getSaleItemId());
        sl.setSaleItemUuid(dto.getSaleItemUuid());
        sl.setUserId(dto.getUserId());
        sl.setNotes(dto.getNotes());
        sl.setCreatedAt(toLdt(dto.getCreatedAt()));
        sl.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        sl.setIsSynced(dto.getIsSynced());
        sl.setDeleted(dto.isDeleted());
        return sl;
    }

    /**
     * Converts a StockLedger model to a StockLedgerDTO. Returns null if the model is null.
     */
    public static StockLedgerDTO toDto(StockLedger sl) {
        if (sl == null) return null;
        StockLedgerDTO dto = new StockLedgerDTO();
        dto.setId(sl.getId());
        dto.setUuid(sl.getUuid());
        dto.setTenantId(sl.getTenantId());
        dto.setProductId(sl.getProductId());
        dto.setProductUuid(sl.getProductUuid());
        dto.setQuantityDelta(sl.getQuantityDelta());
        dto.setReason(sl.getReason());
        dto.setSaleItemId(sl.getsaleItemId());
        dto.setSaleItemUuid(sl.getSaleItemUuid());
        dto.setUserId(sl.getUserId());
        dto.setNotes(sl.getNotes());
        dto.setCreatedAt(toOdt(sl.getCreatedAt()));
        dto.setLastUpdatedAt(toOdt(sl.getLastUpdatedAt()));
        dto.setIsSynced(sl.getIsSynced());
        return dto;
    }

    /* --------------------------------------------------
       CATEGORY
       -------------------------------------------------- */

    /**
     * Converts a CategoryDTO to a Category model. Returns null if the dto is null.
     */
    public static Category fromDto(CategoryDTO dto) {
        if (dto == null) return null;
        Category c = new Category();
        c.setId(dto.getId());
        c.setUuid(dto.getUuid());
        c.setTenantId(dto.getTenantId());
        c.setName(dto.getName());
        c.setDescription(dto.getDescription());
        c.setParentId(dto.getParentId());
        c.setParentUuid(dto.getParentUuid());
        c.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        c.setIsSynced(dto.getIsSynced());
        c.setIsDeleted(dto.isDeleted());
        return c;
    }

    /**
     * Converts a Category model to a CategoryDTO. Returns null if the model is null.
     */
    public static CategoryDTO toDto(Category c) {
        if (c == null) return null;
        CategoryDTO dto = new CategoryDTO();
        dto.setId(c.getId());
        dto.setUuid(c.getUuid());
        dto.setTenantId(c.getTenantId());
        dto.setName(c.getName());
        dto.setDescription(c.getDescription());
        dto.setParentId(c.getParentId());
        dto.setParentUuid(c.getParentUuid());
        dto.setLastUpdatedAt(toOdt(c.getLastUpdatedAt()));
        dto.setIsSynced(c.getIsSynced());
        dto.setDeleted(c.getIsDeleted());
        return dto;
    }

    /* --------------------------------------------------
       UNIT
       -------------------------------------------------- */

    /**
     * Converts a UnitDTO to a Unit model. Returns null if the dto is null.
     */
    public static Unit fromDto(UnitDTO dto) {
        if (dto == null) return null;
        Unit u = new Unit();
        u.setId(dto.getId());
        u.setUuid(dto.getUuid());
        u.setTenantId(dto.getTenantId());
        u.setName(dto.getName());
        u.setAbbreviation(dto.getAbbreviation());
        u.setLastUpdatedAt(toLdt(dto.getLastUpdatedAt()));
        u.setIsSynced(dto.getIsSynced());
        u.setIsDeleted(dto.isDeleted());
        return u;
    }

    /**
     * Converts a Unit model to a UnitDTO. Returns null if the model is null.
     */
    public static UnitDTO toDto(Unit u) {
        if (u == null) return null;
        UnitDTO dto = new UnitDTO();
        dto.setId(u.getId());
        dto.setUuid(u.getUuid());
        dto.setTenantId(u.getTenantId());
        dto.setName(u.getName());
        dto.setAbbreviation(u.getAbbreviation());
        dto.setLastUpdatedAt(toOdt(u.getLastUpdatedAt()));
        dto.setIsSynced(u.getIsSynced());
        dto.setDeleted(u.getIsDeleted());
        return dto;
    }

    /* --------------------------------------------------
       TENANT
       -------------------------------------------------- */

    /**
     * Converts a TenantDTO to a Tenant model. Returns null if the dto is null.
     */
    public static Tenant fromDto(TenantDTO dto) {
        if (dto == null) return null;
        Tenant model = new Tenant();
        model.setId(dto.getId());
        model.setUuid(dto.getUuid());
        model.setLicenseKey(dto.getLicenseKey());
        model.setOwnerEmail(dto.getOwnerEmail());
        model.setStatus(dto.getStatus());
        model.setExpiryDate(dto.getExpiryDate());
        model.setCreatedAt(toLdt(dto.getCreatedAt()));
        return model;
    }

    /**
     * Converts a Tenant model to a TenantDTO. Returns null if the model is null.
     */
    public static TenantDTO toDto(Tenant model) {
        if (model == null) return null;
        TenantDTO dto = new TenantDTO();
        dto.setId(model.getId());
        dto.setUuid(model.getUuid());
        dto.setLicenseKey(model.getLicenseKey());
        dto.setOwnerEmail(model.getOwnerEmail());
        dto.setStatus(model.getStatus());
        dto.setExpiryDate(model.getExpiryDate());
        dto.setCreatedAt(toOdt(model.getCreatedAt()));
        return dto;
    }

    /* --------------------------------------------------
       PRODUCT SUPPLIER
       -------------------------------------------------- */

    /**
     * Converts a ProductSupplierDTO to a ProductSupplier model. Returns null if the dto is null.
     * Note: 'supplierName' is a UI-only field and is not mapped from the DTO.
     */
    public static ProductSupplier fromDto(ProductSupplierDTO dto) {
        if (dto == null) return null;
        ProductSupplier model = new ProductSupplier();
        model.setId(dto.getId());
        model.setUuid(dto.getUuid());
        model.setProductId(dto.getProductId());
        model.setSupplierId(dto.getSupplierId());
        model.setSupplierProductCode(dto.getSupplierProductCode());
        // The 'supplierName' field is intentionally not mapped from the DTO,
        // as it is derived data for UI purposes.
        return model;
    }

    /**
     * Converts a ProductSupplier model to a ProductSupplierDTO. Returns null if the model is null.
     */
    public static ProductSupplierDTO toDto(ProductSupplier model) {
        if (model == null) return null;
        ProductSupplierDTO dto = new ProductSupplierDTO();
        dto.setId(model.getId());
        dto.setUuid(model.getUuid());
        dto.setProductId(model.getProductId());
        dto.setSupplierId(model.getSupplierId());
        dto.setSupplierProductCode(model.getSupplierProductCode());
        // 'supplierName' is not part of the model and thus not mapped to the DTO.
        return dto;
    }
}