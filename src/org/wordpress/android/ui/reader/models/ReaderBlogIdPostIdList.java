package org.wordpress.android.ui.reader.models;

import java.io.Serializable;
import java.util.ArrayList;

public class ReaderBlogIdPostIdList extends ArrayList<ReaderBlogIdPostId>
                                    implements Serializable {

    private static final long serialVersionUID = 0L;

    public ReaderBlogIdPostIdList() {
        super();
    }
    public ReaderBlogIdPostIdList(ArrayList<ReaderBlogIdPostId> list) {
        super();
        if (list != null) {
            for (ReaderBlogIdPostId idPair: list) {
                this.add(idPair);
            }
        }
    }
}
