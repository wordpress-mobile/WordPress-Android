
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

    private long uniqueId;
    private boolean randomEh;
    private String type;
    private int numColumns;
    private ArrayList<Long> ids;

    public MediaGallery(boolean randomEh, String type, int numColumns, ArrayList<Long> ids) {
        this.randomEh = randomEh;
        this.type = type;
        this.numColumns = numColumns;
        this.ids = ids;
        this.uniqueId = System.currentTimeMillis();
    }

    public MediaGallery() {
        randomEh = false;
        type = "";
        numColumns = 3;
        ids = new ArrayList<>();
        this.uniqueId = System.currentTimeMillis();
    }

    public boolean randomEh() {
        return randomEh;
    }

    public void setRandom(boolean randomEh) {
        this.randomEh = randomEh;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getNumColumns() {
        return numColumns;
    }

    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
    }

    public ArrayList<Long> getIds() {
        return ids;
    }

    public String getIdsStr() {
        String ids_str = "";
        if (ids.size() > 0) {
            for (Long id : ids) {
                ids_str += id + ",";
            }
            ids_str = ids_str.substring(0, ids_str.length() - 1);
        }
        return ids_str;
    }

    public void setIds(ArrayList<Long> ids) {
        this.ids = ids;
    }

    /**
     * An id to uniquely identify a media gallery object, so that the same object can be edited in the post editor
     */
    public long getUniqueId() {
        return uniqueId;
    }
}
