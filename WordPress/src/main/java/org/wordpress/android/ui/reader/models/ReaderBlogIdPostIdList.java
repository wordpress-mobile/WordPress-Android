package org.wordpress.android.ui.reader.models;

import java.io.Serializable;
import java.util.ArrayList;

public class ReaderBlogIdPostIdList extends ArrayList<ReaderBlogIdPostId>
                                    implements Serializable {

    private static final long serialVersionUID = 0L;

    public ReaderBlogIdPostIdList() {
        super();
    }

    /*
     * when Android serializes any ArrayList descendant, it does so as an ArrayList
     * rather than its actual class - use this to convert the serialized list back
     * into a ReaderBlogIdPostIdList
     */
    @SuppressWarnings("unused")
    public ReaderBlogIdPostIdList(Serializable serializedList) {
        super();
        if (serializedList != null && serializedList instanceof ArrayList) {
            //noinspection unchecked
            ArrayList<ReaderBlogIdPostId> list = (ArrayList<ReaderBlogIdPostId>) serializedList;
            for (ReaderBlogIdPostId idPair: list) {
                this.add(idPair);
            }
        }
    }

    public int indexOf(long blogId, long postId) {
        for (int i = 0; i < this.size(); i++) {
            if (this.get(i).getBlogId() == blogId && this.get(i).getPostId() == postId) {
                return i;
            }
        }
        return -1;
    }
}
