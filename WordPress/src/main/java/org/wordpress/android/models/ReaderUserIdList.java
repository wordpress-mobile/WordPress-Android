package org.wordpress.android.models;

import java.util.HashSet;

public class ReaderUserIdList extends HashSet<Long> {
    /*
     * returns true if passed list contains the same userIds as this list
     */
    public boolean isSameList(ReaderUserIdList compareIds) {
        if (compareIds==null || compareIds.size()!=this.size())
            return false;
        return this.containsAll(compareIds);
    }
}
