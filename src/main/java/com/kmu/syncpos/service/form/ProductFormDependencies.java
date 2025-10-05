// src/main/java/com/kmu/syncpos/service/form/ProductFormDependencies.java
package com.kmu.syncpos.service.form;

import com.kmu.syncpos.dto.CategoryDTO;
import com.kmu.syncpos.dto.SupplierDTO;
import com.kmu.syncpos.dto.UnitDTO;

import java.util.List;

/**
 * A container class to hold all the data needed to populate the
 * ComboBoxes and other dependent fields on the Product Management form.
 * This cleans up the ProductService by allowing it to return a single,
 * well-defined object.
 */
public class ProductFormDependencies {

    private final List<CategoryDTO> categories;
    private final List<UnitDTO> units;
    private final List<SupplierDTO> suppliers;

    public ProductFormDependencies(List<CategoryDTO> categories, List<UnitDTO> units, List<SupplierDTO> suppliers) {
        this.categories = categories;
        this.units = units;
        this.suppliers = suppliers;
    }

    public List<CategoryDTO> getCategories() {
        return categories;
    }

    public List<UnitDTO> getUnits() {
        return units;
    }

    public List<SupplierDTO> getSuppliers() {
        return suppliers;
    }
}