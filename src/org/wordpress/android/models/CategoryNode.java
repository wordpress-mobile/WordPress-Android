package org.wordpress.android.models;

import android.util.SparseArray;
import org.wordpress.android.WordPress;

import java.util.*;

public class CategoryNode {
    private int categoryId;
    private String name;
    private int parentId;
    private int level;
    SortedMap<String, CategoryNode> children = new TreeMap<String, CategoryNode>(new Comparator<String>() {
        @Override
        public int compare(String s, String s2) {
            return s.compareToIgnoreCase(s2);
        }
    });

    public SortedMap<String, CategoryNode> getChildren() {
        return children;
    }

    public void setChildren(SortedMap<String, CategoryNode> children) {
        this.children = children;
    }

    public CategoryNode(int categoryId, int parentId, String name) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getLevel() {
        return level;
    }

    public static CategoryNode createCategoryTreeFromDB(int blogId) {
        CategoryNode rootCategory = new CategoryNode(-1, -1, "");
        if (WordPress.wpDB == null) {
            return rootCategory;
        }
        List<String> stringCategories = WordPress.wpDB.loadCategories(blogId);

        // First pass instantiate CategoryNode objects
        SparseArray<CategoryNode> categoryMap = new SparseArray<CategoryNode>();
        CategoryNode currentRootNode;
        for (String name : stringCategories) {
            int categoryId = WordPress.wpDB.getCategoryId(blogId, name);
            int parentId = WordPress.wpDB.getCategoryParentId(blogId, name);
            CategoryNode node = new CategoryNode(categoryId, parentId, name);
            categoryMap.put(categoryId, node);
        }

        // Second pass associate nodes to form a tree
        for(int i = 0; i < categoryMap.size(); i++){
            CategoryNode category = categoryMap.valueAt(i);
            if (category.getParentId() == 0) { // root node
                currentRootNode = rootCategory;
            } else {
                currentRootNode = categoryMap.get(category.getParentId(), rootCategory);
            }
            currentRootNode.children.put(category.getName(), categoryMap.get(category.getCategoryId()));
        }
        return rootCategory;
    }

    private static void preOrderTreeTraversal(CategoryNode node, int level, ArrayList<CategoryNode> returnValue) {
        if (node == null) {
            return ;
        }
        if (node.parentId != -1) {
            node.level = level;
            returnValue.add(node);
        }
        for (CategoryNode child : node.getChildren().values()) {
            preOrderTreeTraversal(child, level + 1, returnValue);
        }
    }

    public static ArrayList<CategoryNode> getSortedListOfCategoriesFromRoot(CategoryNode node) {
        ArrayList<CategoryNode> sortedCategories = new ArrayList<CategoryNode>();
        preOrderTreeTraversal(node, 0, sortedCategories);
        return sortedCategories;
    }
}