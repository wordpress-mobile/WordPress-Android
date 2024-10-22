package org.wordpress.android.fluxc.annotations.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String fullEndpoint = getLocalEndpoint().replaceAll("#[^/]*", ""); // Strip any type metadata, e.g. $name#String
        if (getParent() == null) {
            return fullEndpoint;
        }
        return getParent().getFullEndpoint() + fullEndpoint;
    }

    public String getCleanEndpointName() {
        if (getLocalEndpoint().contains(":")) {
            // For 'mixed' endpoints, e.g. item:$theItem, return the label part ('item')
            return getLocalEndpoint().substring(0, getLocalEndpoint().indexOf(":")).replaceAll("-", "_");
        } else {
            return getLocalEndpoint().replaceAll("/|\\$|#.*|(?<!_or)(_ID|_id)|<|>|\\{|\\}", "")
                                     .replaceAll("-", "_")
                                     .replaceAll("\\.", "_");
        }
    }

    public List<String> getEndpointTypes() {
        Pattern pattern = Pattern.compile("#([^\\/]*)");
        Matcher matcher = pattern.matcher(getLocalEndpoint());

        if (matcher.find()) {
            return Arrays.asList(matcher.group(1).split(","));
        }

        return Collections.emptyList();
    }

    public boolean isRoot() {
        return mLocalEndpoint.equals("/");
    }
}
