package org.wordpress.android.fluxc.annotations.endpoint;

import java.util.ArrayList;
import java.util.List;

public class EndpointNode {
    private String mLocalEndpoint;
    private EndpointNode mParent;
    private List<EndpointNode> mChildren;

    public EndpointNode(String localEndpoint) {
        mLocalEndpoint = localEndpoint;
    }

    public String getLocalEndpoint() {
        return mLocalEndpoint;
    }

    public void setLocalEndpoint(String localEndpoint) {
        mLocalEndpoint = localEndpoint;
    }

    public EndpointNode getParent() {
        return mParent;
    }

    public void setParent(EndpointNode parent) {
        mParent = parent;
    }

    public void addChild(EndpointNode child) {
        child.setParent(this);
        if (mChildren == null) {
            mChildren = new ArrayList<>();
        }
        mChildren.add(child);
    }

    public boolean hasChildren() {
        return (mChildren != null && !mChildren.isEmpty());
    }

    public List<EndpointNode> getChildren() {
        return mChildren;
    }

    public EndpointNode getRoot() {
        if (mParent != null) {
            return mParent.getRoot();
        }
        return this;
    }

    public void setChildren(List<EndpointNode> children) {
        mChildren = children;
    }

    public String getFullEndpoint() {
        String fullEndpoint = getLocalEndpoint();
        if (getParent() == null) {
            return fullEndpoint;
        }
       return getParent().getFullEndpoint() + fullEndpoint;
    }

    public String getCleanEndpointName() {
        return getLocalEndpoint().replaceAll("/|\\$|_ID|_id", "").replaceAll("-", "_");
    }

    public boolean isRoot() {
        return mLocalEndpoint.equals("/");
    }
}
