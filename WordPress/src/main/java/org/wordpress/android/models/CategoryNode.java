package org.wordpress.android.models;

import android.util.LongSparseArray;

import org.wordpress.android.fluxc.model.TermModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class CategoryNode {
    private long mCategoryId;
    private String mName;
    private long mParentId;
    private int mLevel;
    private SortedMap<String, CategoryNode> mChildren = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String s, String s2) {
            if (s == null) {
                if (s2 == null) {
                    return 0;
                }
                return 1;
            } else if (s2 == null) {
                return -1;
            }
            return s.compareToIgnoreCase(s2);
        }
    });

    public SortedMap<String, CategoryNode> getChildren() {
        return mChildren;
    }

    public void setChildren(SortedMap<String, CategoryNode> children) {
        this.mChildren = children;
    }

    public CategoryNode(long categoryId, long parentId, String name) {
        this.mCategoryId = categoryId;
        this.mParentId = parentId;
        this.mName = name;
    }

    public long getCategoryId() {
        return mCategoryId;
    }

    public void setCategoryId(int categoryId) {
        this.mCategoryId = categoryId;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public long getParentId() {
        return mParentId;
    }

    public void setParentId(int parentId) {
        this.mParentId = parentId;
    }

    public int getLevel() {
        return mLevel;
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
        for (int i = 0; i < categoryMap.size(); i++) {
            CategoryNode category = categoryMap.valueAt(i);
            if (category.getParentId() == 0) { // root node
                currentRootNode = rootCategory;
            } else {
                currentRootNode = categoryMap.get(category.getParentId(), rootCategory);
            }
            CategoryNode childNode = categoryMap.get(category.getCategoryId());
            if (childNode != null) {
                currentRootNode.mChildren.put(category.getName(), childNode);
            }
        }
        return rootCategory;
    }

    private static void preOrderTreeTraversal(CategoryNode node, int level, ArrayList<CategoryNode> returnValue) {
        if (node == null) {
            return;
        }
        if (node.mParentId != -1) {
            node.mLevel = level;
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
