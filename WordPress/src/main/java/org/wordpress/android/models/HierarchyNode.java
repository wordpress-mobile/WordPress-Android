package org.wordpress.android.models;

import android.text.TextUtils;
import android.util.SparseArray;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class HierarchyNode {
    private int mId;
    private String mName;
    private int mParentId;
    private int mLevel;

    private HierarchyNode mParent;
    SortedMap<String, HierarchyNode> mChildren = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String s, String s2) {
            return s.compareToIgnoreCase(s2);
        }
    });

    public SortedMap<String, HierarchyNode> getChildren() {
        return mChildren;
    }

    public void setChildren(SortedMap<String, HierarchyNode> children) {
        mChildren = children;
    }

    public HierarchyNode getParent() {
        return mParent;
    }

    public void setParent(HierarchyNode parent) {
        mParent = parent;
    }

    public HierarchyNode(int id, int parentId, String name) {
        mId = id;
        mParentId = parentId;
        mName = name;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public int getParentId() {
        return mParentId;
    }

    public void setParentId(int parentId) {
        mParentId = parentId;
    }

    public int getLevel() {
        return mLevel;
    }

    public void setLevel(int level) {
        mLevel = level;
    }

    public enum HierarchyType {
        CATEGORY, PAGE
    }

    public static HierarchyNode createTreeFromDB(int blogId, HierarchyType hierarchyType) {
        HierarchyNode rootNode = new HierarchyNode(-1, -1, "");
        if (WordPress.wpDB == null) {
            return rootNode;
        }

        // First pass instantiate HierarchyNode objects
        SparseArray<HierarchyNode> objectMap = new SparseArray<>();
        HierarchyNode currentRootNode;

        switch (hierarchyType) {
            case CATEGORY:
                List<String> stringCategories = WordPress.wpDB.loadCategories(blogId);

                for (String name : stringCategories) {
                    int categoryId = WordPress.wpDB.getCategoryId(blogId, name);
                    int parentId = WordPress.wpDB.getCategoryParentId(blogId, name);
                    HierarchyNode node = new HierarchyNode(categoryId, parentId, name);
                    objectMap.put(categoryId, node);
                }
                break;
            case PAGE:
                List<PageHierarchyPage> postList = WordPress.wpDB.getPageList(blogId);

                for (PageHierarchyPage page : postList) {
                    int postId = Integer.parseInt(page.getRemotePostId());
                    int parentId = Integer.parseInt(page.getPageParentId());
                    String title = page.getTitle();
                    if (TextUtils.isEmpty(title)) {
                        title = "#" + postId + " (" + WordPress.getContext().getString(R.string.untitled) + ")";
                    }

                    HierarchyNode node = new HierarchyNode(postId, parentId, title);
                    objectMap.put(postId, node);
                }
                break;
        }

        // Second pass associate nodes to form a tree
        for(int i = 0; i < objectMap.size(); i++){
            HierarchyNode hierarchyNode = objectMap.valueAt(i);
            if (hierarchyNode.getParentId() == 0) { // root node
                currentRootNode = rootNode;
            } else {
                currentRootNode = objectMap.get(hierarchyNode.getParentId(), rootNode);
                hierarchyNode.setParent(objectMap.get(hierarchyNode.getParentId()));
            }
            currentRootNode.getChildren().put(hierarchyNode.getName(), objectMap.get(hierarchyNode.getId()));
        }
        return rootNode;
    }

    private static void preOrderTreeTraversal(HierarchyNode node, int level, ArrayList<HierarchyNode> returnValue) {
        if (node == null) {
            return ;
        }
        if (node.getParentId() != -1) {
            node.setLevel(level);
            returnValue.add(node);
        }
        for (HierarchyNode child : node.getChildren().values()) {
            preOrderTreeTraversal(child, level + 1, returnValue);
        }
    }

    public static ArrayList<HierarchyNode> getSortedListFromRoot(HierarchyNode node) {
        ArrayList<HierarchyNode> sortedNodes = new ArrayList<>();
        preOrderTreeTraversal(node, 0, sortedNodes);
        return sortedNodes;
    }

    public boolean isDescendantOfId(int id) {
        if (mParentId == id) {
            return true;
        }
        return (mParentId != 0 && mParent.isDescendantOfId(id));
    }
}