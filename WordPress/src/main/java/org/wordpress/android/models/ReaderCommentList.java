package org.wordpress.android.models;

import org.wordpress.android.datasets.ReaderCommentTable;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;

public class ReaderCommentList extends ArrayList<ReaderComment> {

    public ReaderCommentList(){}

    public ReaderCommentList(int initialCapacity){
        super(initialCapacity);
    }

    private boolean commentIdExists(long commentId) {
        return (indexOfCommentId(commentId) > -1);
    }

    public int indexOfCommentId(long commentId) {
        for (int i=0; i < this.size(); i++) {
            if (commentId==this.get(i).commentId)
                return i;
        }
        return -1;
    }

    /*
     * does passed list contain the same comments as this list?
     */
    public boolean isSameList(ReaderCommentList comments) {
        if (comments==null || comments.size()!=this.size())
            return false;

        for (ReaderComment comment: comments) {
            if (!commentIdExists(comment.commentId))
                return false;
        }

        return true;
    }

    public boolean replaceComment(long commentId, ReaderComment newComment) {
        if (newComment == null) {
            return false;
        }

        int index = indexOfCommentId(commentId);
        if (index == -1) {
            return false;
        }

        // make sure the new comment has the same level as the old one
        newComment.level = this.get(index).level;

        this.set(index, newComment);
        return true;
    }

    /*
     * builds a new list from the passed one with child comments placed under their parents and indent levels applied
     * NOTE: list should be sorted according to timestamp
     */
    public static ReaderCommentList getLevelList(ReaderCommentList list,
                                                 @ReaderCommentTable.CommentsOrderBy int commentsOrder){
        switch(commentsOrder){
            case ReaderCommentTable.ORDER_BY_NEWEST_COMMENT_FIRST:
                return getNewestFirstLevelList(list);

            case ReaderCommentTable.ORDER_BY_TIME_OF_COMMENT:
                return getOldestFirstLevelList(list);
        }
        throw new IllegalArgumentException("Unknown commentsOrder");
    }

    private static ReaderCommentList getNewestFirstLevelList(ReaderCommentList list){
        ReaderCommentList resultList = new ReaderCommentList(list.size());

        for(int i=0; i < list.size() ; i++){
            putNewestFirstComment(list, resultList, i);
        }

        return resultList;
    }

    /**
     * first add parents of current comment(at index) from oldList to newList than add current comment
     */
    private static int putNewestFirstComment(ReaderCommentList oldList,
                                              ReaderCommentList newList,
                                              int index){
        ReaderComment comment = oldList.get(index);
        int level = 1;

        if(comment.parentId != 0){
            int parentIndex = oldList.indexOfCommentId(comment.parentId);
            if(parentIndex >= 0){
                level = putNewestFirstComment(oldList, newList, parentIndex) + 1;
                //NOTE: parentIndex can changed if parent comment of this parent is removed so always find fresh index of parent
                oldList.remove(oldList.indexOfCommentId(comment.parentId));
            }else{
                AppLog.d(AppLog.T.READER, "Parent Comment Not in list , parentId : "+comment.parentId);
            }
        }

        comment.level = level;
        newList.add(comment);
        return level;
    }

    private static ReaderCommentList getOldestFirstLevelList(ReaderCommentList oldList){
        for(int i=0; i < oldList.size() ; i++){
            ReaderComment comment = oldList.get(i);
            if(comment.parentId == 0){
                continue;
            }

            int parentIndex = oldList.indexOfCommentId(comment.parentId);

            if( parentIndex < 0 ){
                AppLog.d(AppLog.T.READER, "Parent Comment Not in list , parentId : "+comment.parentId);
                continue;
            }

            ReaderComment parentComment = oldList.get(parentIndex);

            // swap parent and child comment such that child comment come before parent comment
            // Due to which putNewestFirstComment() will put parent comment before child and than put child in new list
            // when it encouters a child comment in oldList
            oldList.set(i, parentComment);
            oldList.set(parentIndex, comment);
        }

        return getNewestFirstLevelList(oldList);
    }
}
