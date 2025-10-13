// src/main/java/com/kmu/syncpos/controllers/CategoryManagementController.java
package com.kmu.syncpos.controllers;

import com.kmu.syncpos.dto.CategoryDTO;
import com.kmu.syncpos.models.Category;
import com.kmu.syncpos.service.CategoryService;
import com.kmu.syncpos.util.ModelMapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CategoryManagementController {

    @FXML private Button addRootButton, addSubButton, saveButton, deleteButton, changeParentButton;
    @FXML private TreeView<Category> categoryTreeView;
    @FXML private Label detailsLabel, parentCategoryLabel;
    @FXML private GridPane detailsPane;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;

    private final CategoryService categoryService = new CategoryService();
    private Category selectedCategory = null;
    private Long newParentId = null;
    private String newParentUuid = null;

    @FXML
    public void initialize() {
        TreeItem<Category> rootItem = new TreeItem<>();
        rootItem.setExpanded(true);
        categoryTreeView.setRoot(rootItem);
        categoryTreeView.setShowRoot(false);

        buildCategoryTree();
        setupEventListeners();
        clearForm(true);
    }

    private void setupEventListeners() {
        categoryTreeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        selectedCategory = newValue.getValue();
                        populateForm(newValue);
                    } else {
                        selectedCategory = null;
                        clearForm(true);
                    }
                }
        );
    }

    private void buildCategoryTree() {
        List<CategoryDTO> categoryDTOs = categoryService.getAllActiveCategories();
        List<Category> categories = categoryDTOs.stream()
                .map(ModelMapper::fromDto)
                .collect(Collectors.toList());

        categoryTreeView.getRoot().getChildren().clear();
        Map<Long, TreeItem<Category>> treeItemMap = new HashMap<>();

        for (Category category : categories) {
            treeItemMap.put(category.getId(), new TreeItem<>(category));
        }

        for (Category category : categories) {
            TreeItem<Category> currentItem = treeItemMap.get(category.getId());
            Long parentId = category.getParentId();

            if (parentId == null || parentId == 0) {
                categoryTreeView.getRoot().getChildren().add(currentItem);
            } else {
                TreeItem<Category> parentItem = treeItemMap.get(parentId);
                if (parentItem != null) {
                    parentItem.getChildren().add(currentItem);
                } else {
                    categoryTreeView.getRoot().getChildren().add(currentItem); // Orphan
                }
            }
        }
    }

    private void populateForm(TreeItem<Category> selectedTreeItem) {
        detailsLabel.setText("Editing: " + selectedTreeItem.getValue().getName());
        detailsPane.setVisible(true);
        nameField.setText(selectedTreeItem.getValue().getName());
        descriptionArea.setText(selectedTreeItem.getValue().getDescription());

        if (selectedTreeItem.getParent() != null && selectedTreeItem.getParent() != categoryTreeView.getRoot()) {
            parentCategoryLabel.setText("Parent: " + selectedTreeItem.getParent().getValue().getName());
        } else {
            parentCategoryLabel.setText("Parent: None (Root Category)");
        }
        // Manage button visibility
        saveButton.setVisible(true);
        deleteButton.setVisible(true);
        changeParentButton.setVisible(true);
        addSubButton.setDisable(false);
    }

    private void clearForm(boolean hidePane) {
        detailsPane.setVisible(!hidePane);
        if(!hidePane) {
            detailsLabel.setText("Add New Root Category");
            parentCategoryLabel.setText("Parent: None (Root Category)");
            saveButton.setVisible(true); // Show save button when adding
        } else {
            detailsLabel.setText("Select a category to view details, or add a new one.");
            saveButton.setVisible(false);
        }
        selectedCategory = null;
        newParentId = null;
        newParentUuid = null;
        nameField.clear();
        descriptionArea.clear();
        categoryTreeView.getSelectionModel().clearSelection();

        // Manage button visibility/state
        deleteButton.setVisible(false);
        changeParentButton.setVisible(false);
        addSubButton.setDisable(true);
    }

    @FXML
    private void handleAddRootButton() {
        clearForm(false);
    }

    @FXML
    private void handleAddSubButton() {
        TreeItem<Category> selectedParentItem = categoryTreeView.getSelectionModel().getSelectedItem();
        if (selectedParentItem == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a parent category first.");
            return;
        }
        clearForm(false);
        detailsLabel.setText("Adding Sub-category to: " + selectedParentItem.getValue().getName());
        parentCategoryLabel.setText("Parent: " + selectedParentItem.getValue().getName());
        newParentId = selectedParentItem.getValue().getId();
        newParentUuid = selectedParentItem.getValue().getUuid();
    }

    @FXML
    private void handleSave() {
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Category name cannot be empty.");
            return;
        }

        CategoryDTO dto;
        if (selectedCategory != null) { // Updating
            dto = ModelMapper.toDto(selectedCategory);
        } else { // Creating
            dto = new CategoryDTO();
            dto.setParentId(newParentId);
            dto.setParentUuid(newParentUuid);
        }

        dto.setName(nameField.getText().trim());
        dto.setDescription(descriptionArea.getText().trim());

        categoryService.saveCategory(dto);
        buildCategoryTree();
        clearForm(true);
    }

    @FXML
    private void handleDelete() {
        if (selectedCategory == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete category '" + selectedCategory.getName() + "'?");
        confirmation.setContentText("Warning: Any sub-categories will become root categories. This cannot be undone.");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            categoryService.deleteCategory(selectedCategory.getId());
            buildCategoryTree();
            clearForm(true);
        }
    }

    @FXML
    private void handleChangeParent() {
        if (selectedCategory == null) return;

        // 1. Get DTOs from the service
        List<CategoryDTO> categoryDTOs = categoryService.getAllActiveCategories().stream()
                .filter(c -> c.getId() != selectedCategory.getId()) // Can't be its own parent
                .collect(Collectors.toList());

        // 2. Convert DTOs to Models for use in the UI Dialog
        List<Category> choices = categoryDTOs.stream()
                .map(ModelMapper::fromDto)
                .collect(Collectors.toList());

        // 3. Create the "None (Root)" option as a Model object
        Category rootOption = new Category();
        rootOption.setId(0L); // Use a special ID for root
        rootOption.setName("None (Root Category)");
        choices.add(0, rootOption);

        // 4. Create the dialog with the list of Category MODELS
        ChoiceDialog<Category> dialog = new ChoiceDialog<>(rootOption, choices);
        dialog.setTitle("Change Parent");
        dialog.setHeaderText("Select a new parent for '" + selectedCategory.getName() + "'");
        dialog.setContentText("New Parent:");
        dialog.getComboBox().setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category == null ? "" : category.getName();
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        });

        Optional<Category> result = dialog.showAndWait();
        result.ifPresent(newParent -> {
            // 6. Process the resulting Category MODEL to get the ID and UUID
            Long newParentId = (newParent.getId() == 0L) ? null : newParent.getId();
            String newParentUuid = (newParent.getId() == 0L) ? null : newParent.getUuid();

            // Call the service to perform the update
            categoryService.changeCategoryParent(selectedCategory.getId(), newParentId, newParentUuid);

            buildCategoryTree();
            clearForm(true);
        });
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}