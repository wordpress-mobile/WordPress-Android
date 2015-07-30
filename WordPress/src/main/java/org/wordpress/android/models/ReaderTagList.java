package org.wordpress.android.models;

import java.util.ArrayList;

public class ReaderTagList extends ArrayList<ReaderTag> {

    @Override
    public Object clone() {
        return super.clone();
    }

    public int indexOfTagName(String tagName) {
        if (tagName == null || isEmpty()) {
            return -1;
        }

        for (int i = 0; i < size(); i++) {
            if (tagName.equals(this.get(i).getTagName())) {
                return i;
            }
        }

        return -1;
    }

    private int indexOfTag(ReaderTag tag) {
        if (tag == null || isEmpty()) {
            return -1;
        }

        for (int i = 0; i < this.size(); i++) {
            if (ReaderTag.isSameTag(tag, this.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public boolean isSameList(ReaderTagList tagList) {
        if (tagList == null || tagList.size() != this.size()) {
            return false;
        }

        for (ReaderTag tag: tagList) {
            int i = indexOfTag(tag);
            if (i == -1) {
                return false;
            } else if (!tag.getEndpoint().equals(this.get(i).getEndpoint())) {
                return false;
            }
        }

        return true;
    }

    /*
     * returns a list of tags that are in this list but not in the passed list
     */
    public ReaderTagList getDeletions(ReaderTagList tagList) {
        ReaderTagList deletions = new ReaderTagList();
        if (tagList == null) {
            return deletions;
        }

        for (ReaderTag tag: this) {
            if (indexOfTag(tag) == -1) {
                deletions.add(tag);
            }
        }

        return deletions;
    }
}
