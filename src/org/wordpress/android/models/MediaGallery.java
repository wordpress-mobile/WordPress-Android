
package org.wordpress.android.models;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A model representing a Media Gallery.
 * A unique id is not used on the website, but only in this app.
 * It is used to uniquely determining the instance of the object, as it is 
 * passed between post and media gallery editor.  
 */
public class MediaGallery implements Serializable{

    private static final long serialVersionUID = 2359176987182027508L;
    
    private long uniqueId;
    private boolean isRandom;
    private String type;
    private int numColumns;
    private ArrayList<String> ids;

    public MediaGallery(boolean isRandom, String type, int numColumns, ArrayList<String> ids) {
        this.isRandom = isRandom;
        this.type = type;
        this.numColumns = numColumns;
        this.ids = ids;
        this.uniqueId = System.currentTimeMillis();
    }
    
    public MediaGallery() {
        isRandom = false;
        type = "";
        numColumns = 3;
        ids = new ArrayList<String>();
        this.uniqueId = System.currentTimeMillis();
    }

    public boolean isRandom() {
        return isRandom;
    }

    public void setRandom(boolean isRandom) {
        this.isRandom = isRandom;
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

    public ArrayList<String> getIds() {
        return ids;
    }

    public String getIdsStr() {
        String ids_str = "";
        if (ids.size() > 0) {
            for (String id : ids) {
                ids_str += id + ","; 
            }
            ids_str = ids_str.substring(0, ids_str.length() - 1);
        }
        return ids_str;
    }
    
    public void setIds(ArrayList<String> ids) {
        this.ids = ids;
    }
    
    /** An id to uniquely identify a media gallery object, so that the same object can be edited in the post editor **/
    public long getUniqueId() {
        return uniqueId;
    }

}
