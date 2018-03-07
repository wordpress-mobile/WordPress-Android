package org.wordpress.android.util.helpers;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A model representing a Media Gallery.
 * A unique id is not used on the website, but only in this app.
 * It is used to uniquely determining the instance of the object, as it is
 * passed between post and media gallery editor.
 */
public class MediaGallery implements Serializable {
    private static final long serialVersionUID = 2359176987182027508L;

    private long mUniqueId;
    private boolean mIsRandom;
    private String mType;
    private int mNumColumns;
    private ArrayList<Long> mIds;

    public MediaGallery(boolean isRandom, String type, int numColumns, ArrayList<Long> ids) {
        mIsRandom = isRandom;
        mType = type;
        mNumColumns = numColumns;
        mIds = ids;
        mUniqueId = System.currentTimeMillis();
    }

    public MediaGallery() {
        mIsRandom = false;
        mType = "";
        mNumColumns = 3;
        mIds = new ArrayList<>();
        mUniqueId = System.currentTimeMillis();
    }

    public boolean isRandom() {
        return mIsRandom;
    }

    public void setRandom(boolean isRandom) {
        this.mIsRandom = isRandom;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public void setNumColumns(int numColumns) {
        this.mNumColumns = numColumns;
    }

    public ArrayList<Long> getIds() {
        return mIds;
    }

    public String getIdsStr() {
        String idsStr = "";
        if (mIds.size() > 0) {
            for (Long id : mIds) {
                idsStr += id + ",";
            }
            idsStr = idsStr.substring(0, idsStr.length() - 1);
        }
        return idsStr;
    }

    public void setIds(ArrayList<Long> ids) {
        this.mIds = ids;
    }

    /**
     * An id to uniquely identify a media gallery object, so that the same object can be edited in the post editor
     */
    public long getUniqueId() {
        return mUniqueId;
    }
}
