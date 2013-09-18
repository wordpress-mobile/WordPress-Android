package org.wordpress.android.models;

import java.util.ArrayList;

/**
 * Created by nbradbury on 6/23/13.
 */
public class ReaderTopicList extends ArrayList<ReaderTopic> {

    public int indexOfTopic(String topicName) {
        if (topicName==null || isEmpty())
            return -1;
        for (int i=0; i < size(); i++) {
            if (topicName.equalsIgnoreCase(this.get(i).getTopicName()))
                return i;
        }
        return -1;
    }

    public boolean isSameList(ReaderTopicList topics) {
        if (topics==null || topics.size()!=this.size())
            return false;
        for (ReaderTopic topic: topics) {
            if (indexOfTopic(topic.getTopicName())==-1)
                return false;
        }
        return true;
    }

    /*
     * returns a list of topics that are in this list but not in the passed list
     */
    public ReaderTopicList getDeletions(ReaderTopicList topics) {
        ReaderTopicList deletions = new ReaderTopicList();
        if (topics==null)
            return deletions;
        for (ReaderTopic thisTopic: this) {
            if (topics.indexOfTopic(thisTopic.getTopicName()) == -1)
                deletions.add(thisTopic);
        }
        return deletions;
    }

}
