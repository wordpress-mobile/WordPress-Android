package org.wordpress.android.models;

public class Category {
    private int categoryId;
    private int parentId;
    private String name;

    public Category(int categoryId, int parentId, String name) {
        this.categoryId = categoryId;
        this.parentId = parentId;
        this.name = name;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}