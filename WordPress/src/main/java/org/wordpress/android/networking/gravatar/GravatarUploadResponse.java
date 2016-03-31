package org.wordpress.android.networking.gravatar;

public class GravatarUploadResponse {
    public static class Entry {
        public File file;
    }

    public static class File {
        public String name;
        public String type;
        public int error;
        public int size;
        public String reportedType;
        public int typeid;
    }

    public Entry entry;
}