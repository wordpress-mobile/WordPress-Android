package org.wordpress.android.models;

import java.util.ArrayList;

public class ReaderTagList extends ArrayList<ReaderTag> {

    public int indexOfTag(ReaderTag tag) {
        if (tag == null || isEmpty()) {
            return -1;
        }

        for (int i = 0; i < size(); i++) {
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

        for (ReaderTag thisTag: tagList) {
            if (indexOfTag(thisTag) == -1) {
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
            if (tagList.indexOfTag(thisTag) == -1) {
                deletions.add(thisTag);
            }
        }

        return deletions;
    }
}
