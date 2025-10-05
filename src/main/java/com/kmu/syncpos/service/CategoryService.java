// src/main/java/com/kmu/syncpos/service/CategoryService.java
package com.kmu.syncpos.service;

import com.kmu.syncpos.auth.TenantContext;
import com.kmu.syncpos.dao.CategoryDAO;
import com.kmu.syncpos.dto.CategoryDTO;

import java.util.Collections;
import java.util.List;

public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService() {
        this.categoryDAO = new CategoryDAO();
    }

    public List<CategoryDTO> getAllActiveCategories() {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return categoryDAO.getAll(tenantId);
        } catch (IllegalStateException e) {
            System.err.println("CategoryService: Tenant context not available.");
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a single category by its ID.
     * @param categoryId The ID of the category to find.
     * @return A CategoryDTO if found, otherwise null.
     */
    public CategoryDTO getCategoryById(long categoryId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            return categoryDAO.getById(categoryId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("CategoryService: Tenant context not available.");
            return null;
        }
    }

    public void saveCategory(CategoryDTO dto) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            if (dto.getId() == 0) {
                categoryDAO.createLocal(dto, tenantId);
            } else {
                categoryDAO.updateLocal(dto, tenantId);
            }
        } catch (IllegalStateException e) {
            System.err.println("CategoryService: Tenant context not available.");
        }
    }

    /**
     * Changes the parent of an existing category.
     * @param categoryId The ID of the category to move.
     * @param newParentId The ID of the new parent (can be null for a root category).
     * @param newParentUuid The UUID of the new parent (can be null for a root category).
     */
    public void changeCategoryParent(long categoryId, Long newParentId, String newParentUuid) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            CategoryDTO category = categoryDAO.getById(categoryId, tenantId);
            if (category != null) {
                category.setParentId(newParentId);
                category.setParentUuid(newParentUuid);
                categoryDAO.updateLocal(category, tenantId);
            } else {
                System.err.println("CategoryService: Could not change parent, category with ID " + categoryId + " not found.");
            }
        } catch (IllegalStateException e) {
            System.err.println("CategoryService: Tenant context not available. " + e.getMessage());
        }
    }

    public void deleteCategory(long categoryId) {
        try {
            String tenantId = TenantContext.getTenant().getUuid();
            categoryDAO.markAsDeleted(categoryId, tenantId);
        } catch (IllegalStateException e) {
            System.err.println("CategoryService: Tenant context not available.");
        }
    }

    public List<CategoryDTO> getUnsyncedCategories(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Collections.emptyList();
        }
        return categoryDAO.getUnsynced(tenantId);
    }
}