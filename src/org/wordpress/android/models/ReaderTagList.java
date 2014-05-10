package org.wordpress.android.models;

import java.util.ArrayList;

public class ReaderTagList extends ArrayList<ReaderTag> {
    public int indexOfTag(String tagName) {
        if (tagName==null || isEmpty())
            return -1;
        for (int i=0; i < size(); i++) {
            if (tagName.equalsIgnoreCase(this.get(i).getTagName()))
                return i;
        }
        return -1;
    }

    public boolean isSameList(ReaderTagList tags) {
        if (tags==null || tags.size()!=this.size())
            return false;
        for (ReaderTag thisTag: tags) {
            if (indexOfTag(thisTag.getTagName())==-1)
                return false;
        }
        return true;
    }

    /*
     * returns a list of tags that are in this list but not in the passed list
     */
    public ReaderTagList getDeletions(ReaderTagList tags) {
        ReaderTagList deletions = new ReaderTagList();
        if (tags==null)
            return deletions;
        for (ReaderTag thisTag: this) {
            if (tags.indexOfTag(thisTag.getTagName()) == -1)
                deletions.add(thisTag);
        }
        return deletions;
    }
}
