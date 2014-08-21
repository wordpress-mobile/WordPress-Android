package org.wordpress.android.models;

import java.util.ArrayList;

public class ReaderTagList extends ArrayList<ReaderTag> {

    public int indexOfTagName(final String tagName) {
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

    private boolean hasSameTag(ReaderTag tag) {
        if (tag == null || isEmpty()) {
            return false;
        }

        for (ReaderTag thisTag : this) {
            if (ReaderTag.isSameTag(thisTag, tag)) {
                return true;
            }
        }

        return false;
    }

    public boolean isSameList(ReaderTagList tagList) {
        if (tagList == null || tagList.size() != this.size()) {
            return false;
        }

        for (ReaderTag thisTag: tagList) {
            if (!hasSameTag(thisTag)) {
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

        for (ReaderTag thisTag: this) {
            if (!tagList.hasSameTag(thisTag)) {
                deletions.add(thisTag);
            }
        }

        return deletions;
    }
}
