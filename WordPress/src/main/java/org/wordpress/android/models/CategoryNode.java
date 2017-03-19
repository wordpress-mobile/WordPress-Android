package org.wordpress.android.models;

import android.util.LongSparseArray;

import org.wordpress.android.fluxc.model.TermModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class CategoryNode {
    private long categoryId;
    private String name;
    private long parentId;
    private int level;
    private SortedMap<String, CategoryNode> children = new TreeMap<>(new Comparator<String>() {
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

    public CategoryNode(long categoryId, long parentId, String name) {
        this.categoryId = categoryId;
        this.parentId = parentId;
        this.name = name;
    }

    public long getCategoryId() {
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

    public long getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getLevel() {
        return level;
    }

    public static CategoryNode createCategoryTreeFromList(List<TermModel> categories) {
        CategoryNode rootCategory = new CategoryNode(-1, -1, "");

        // First pass instantiate CategoryNode objects
        LongSparseArray<CategoryNode> categoryMap = new LongSparseArray<>();
        CategoryNode currentRootNode;
        for (TermModel category : categories) {
            long categoryId = category.getRemoteTermId();
            long parentId = category.getParentRemoteId();
            CategoryNode node = new CategoryNode(categoryId, parentId, category.getName());
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
        ArrayList<CategoryNode> sortedCategories = new ArrayList<>();
        preOrderTreeTraversal(node, 0, sortedCategories);
        return sortedCategories;
    }
}