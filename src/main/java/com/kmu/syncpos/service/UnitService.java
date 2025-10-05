// src/main/java/com/kmu/syncpos/service/UnitService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.UnitDAO;
import com.kmu.syncpos.dto.UnitDTO;

import java.util.Collections;
import java.util.List;

public class UnitService {

    private final UnitDAO unitDAO;

    public UnitService() {
        this.unitDAO = new UnitDAO();
    }

    /**
     * Gets all non-deleted units for the current tenant.
     * @return A list of UnitDTOs.
     */
    public List<UnitDTO> getAllActiveUnits() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return unitDAO.getAll(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("UnitService: Cannot get units, tenant context not available.");
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a single unit by its ID.
     * @param unitId The ID of the unit to retrieve.
     * @return A UnitDTO if found, otherwise null.
     */
    public UnitDTO getUnitById(long unitId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return unitDAO.getById(unitId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("UnitService: Tenant context not available.");
            return null;
        }
    }

    /**
     * Saves a unit. Handles both create and update operations.
     * @param dto The unit data to save.
     */
    public void saveUnit(UnitDTO dto) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            if (dto.getId() == 0) {
                unitDAO.createLocal(dto, tenantId);
            } else {
                unitDAO.updateLocal(dto, tenantId);
            }
        } catch (IllegalStateException e) {
            System.err.println("UnitService: Cannot save unit, tenant context not available.");
        }
    }

    /**
     * Deletes a unit for the current tenant. This should be a soft delete.
     * @param unitId The ID of the unit to delete.
     */
    public void deleteUnit(long unitId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            unitDAO.markAsDeleted(unitId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("UnitService: Cannot delete unit, tenant context not available.");
        }
    }

    public List<UnitDTO> getUnsyncedUnits(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return unitDAO.getUnsynced(tenantId);
    }
}