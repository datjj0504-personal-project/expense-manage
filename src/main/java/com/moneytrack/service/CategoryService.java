package com.moneytrack.service;

import com.moneytrack.model.Category;
import com.moneytrack.model.TransactionType;
import com.moneytrack.repository.CategoryRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<Category> findAll() throws SQLException {
        return repository.findAll();
    }

    public Category save(long id, String name, TransactionType type) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Enter a category name.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Select a category type.");
        }
        Category category = new Category(id, name.strip(), type);
        if (id == 0) {
            return repository.create(category);
        }
        if (!repository.update(category)) {
            throw new IllegalStateException("This category no longer exists. Refresh and try again.");
        }
        return category;
    }

    public void delete(long id) throws SQLException {
        if (!repository.delete(id)) {
            throw new IllegalStateException("This category no longer exists. Refresh and try again.");
        }
    }
}
