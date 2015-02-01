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

public class PageNode {
    private int pageId;
    private String name;
    private int parentId;
    private int level;

    private PageNode parent;
    SortedMap<String, PageNode> children = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String s, String s2) {
            return s.compareToIgnoreCase(s2);
        }
    });

    public SortedMap<String, PageNode> getChildren() {
        return children;
    }

    public void setChildren(SortedMap<String, PageNode> children) {
        this.children = children;
    }

    public PageNode getParent() {
        return parent;
    }

    public void setParent(PageNode parent) {
        this.parent = parent;
    }

    public PageNode(int pageId, int parentId, String name) {
        this.pageId = pageId;
        this.parentId = parentId;
        this.name = name;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
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

    public static PageNode createPageTreeFromDB(int blogId) {
        PageNode rootPage = new PageNode(-1, -1, "");
        if (WordPress.wpDB == null) {
            return rootPage;
        }

        // First pass instantiate PageNode objects
        SparseArray<PageNode> postsMap = new SparseArray<>();
        PageNode currentRootNode;

        List<PageHierarchyPage> postList = WordPress.wpDB.getPageList(blogId);

        for (PageHierarchyPage page : postList) {
            int postId = Integer.parseInt(page.getRemotePostId());
            int parentId = Integer.parseInt(page.getPageParentId());
            String title = page.getTitle();
            if (TextUtils.isEmpty(title)) {
                title = "#" + postId + " (" + WordPress.getContext().getString(R.string.untitled) + ")";
            }

            PageNode node = new PageNode(postId, parentId, title);
            postsMap.put(postId, node);
        }

        // Second pass associate nodes to form a tree
        for(int i = 0; i < postsMap.size(); i++){
            PageNode page = postsMap.valueAt(i);
            if (page.getParentId() == 0) { // root node
                currentRootNode = rootPage;
            } else {
                currentRootNode = postsMap.get(page.getParentId(), rootPage);
                page.parent = postsMap.get(page.getParentId());
            }
            currentRootNode.children.put(page.getName(), postsMap.get(page.getPageId()));
        }
        return rootPage;
    }

    private static void preOrderTreeTraversal(PageNode node, int level, ArrayList<PageNode> returnValue) {
        if (node == null) {
            return ;
        }
        if (node.parentId != -1) {
            node.level = level;
            returnValue.add(node);
        }
        for (PageNode child : node.getChildren().values()) {
            preOrderTreeTraversal(child, level + 1, returnValue);
        }
    }

    public static ArrayList<PageNode> getSortedListOfPagesFromRoot(PageNode node) {
        ArrayList<PageNode> sortedPages = new ArrayList<>();
        preOrderTreeTraversal(node, 0, sortedPages);
        return sortedPages;
    }

    public boolean isDescendantOfPageWithId(int postId) {
        if (parentId == postId) {
            return true;
        }
        return (parentId != 0 && parent.isDescendantOfPageWithId(postId));
    }
}