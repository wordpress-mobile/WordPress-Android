package org.wordpress.android.models;

import java.util.ArrayList;

public class ReaderTagList extends ArrayList<ReaderTag> {

    public int indexOfTagName(String tagName) {
        if (tagName == null || isEmpty()) {
            return -1;
        }

        for (int i = 0; i < size(); i++) {
            if (tagName.equals(this.get(i).getTagSlug())) {
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

    public boolean isSameList(ReaderTagList otherList) {
        if (otherList == null || otherList.size() != this.size()) {
            return false;
        }

        for (ReaderTag otherTag: otherList) {
            int i = this.indexOfTag(otherTag);
            if (i == -1) {
                return false;
            } else if (!otherTag.getEndpoint().equals(this.get(i).getEndpoint())) {
                return false;
            }
        }

        return true;
    }

    /*
     * returns a list of tags that are in this list but not in the passed list
     */
    public ReaderTagList getDeletions(ReaderTagList otherList) {
        ReaderTagList deletions = new ReaderTagList();
        if (otherList == null) {
            return deletions;
        }

        for (ReaderTag thisTag: this) {
            if (otherList.indexOfTag(thisTag) == -1) {
                deletions.add(thisTag);
            }
        }

        return deletions;
    }
}
