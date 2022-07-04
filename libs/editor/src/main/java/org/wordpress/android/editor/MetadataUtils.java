package org.wordpress.android.editor;

import android.text.TextUtils;

import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.aztec.AztecAttributes;
import org.wordpress.aztec.plugins.shortcodes.extensions.VideoPressExtensionsKt;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MetadataUtils {
    public static JSONObject getMetadata(AttributesWithClass attrs, int naturalWidth, int naturalHeight) {
        JSONObject metadata = new JSONObject();
        addMetadataProperty(metadata, "align", "none"); // Accepted values: center, left, right or empty string.
        addMetadataProperty(metadata, "alt", ""); // Image alt attribute
        // Numeric attachment id of the image in the site's media library
        addMetadataProperty(metadata, "attachment_id", "");
        addMetadataProperty(metadata, "caption", ""); // The text of the caption for the image (if any)
        addMetadataProperty(metadata, "captionClassName", ""); // The classes for the caption shortcode (if any).
        // The caption shortcode's ID attribute. The numeric value should match the value of attachment_id
        addMetadataProperty(metadata, "captionId", "");
        // The class attribute for the image. Does not include editor generated classes
        addMetadataProperty(metadata, "classes", "");
        addMetadataProperty(metadata, "height", ""); // The image height attribute
        addMetadataProperty(metadata, "linkClassName", ""); // The class attribute for the link
        addMetadataProperty(metadata, "linkRel", ""); // The rel attribute for the link (if any)
        addMetadataProperty(metadata, "linkTargetBlank", false); // true if the link should open in a new window.
        addMetadataProperty(metadata, "linkUrl", ""); // The href attribute of the link
        // Accepted values: size-thumbnail, size-medium, size-large, size-full
        addMetadataProperty(metadata, "size", "size-full");
        addMetadataProperty(metadata, "src", ""); // The src attribute of the image
        addMetadataProperty(metadata, "title", ""); // The title attribute of the image (if any)
        addMetadataProperty(metadata, "width", ""); // The image width attribute
        addMetadataProperty(metadata, "naturalWidth", ""); // The natural width of the image.
        addMetadataProperty(metadata, "naturalHeight", ""); // The natural height of the image.

        String src = attrs.getAttribute("src", "");
        if (TextUtils.isEmpty(src)) {
            src = attrs.getAttribute(VideoPressExtensionsKt.getATTRIBUTE_VIDEOPRESS_HIDDEN_SRC(), "");
        }
        addMetadataProperty(metadata, "src", src);

        addMetadataProperty(metadata, "alt", attrs.getAttribute("alt", ""));
        addMetadataProperty(metadata, "title", attrs.getAttribute("title", ""));
        addMetadataProperty(metadata, "naturalWidth", naturalWidth);
        addMetadataProperty(metadata, "naturalHeight", naturalHeight);

        Pattern isIntRegExp = Pattern.compile("^\\d+$");

        String width = attrs.getAttribute("width", "");
        if (!isIntRegExp.matcher(width).matches() || NumberUtils.toInt(width) == 0) {
            addMetadataProperty(metadata, "width", naturalWidth);
        } else {
            addMetadataProperty(metadata, "width", width);
        }

        String height = attrs.getAttribute("height", "");
        if (!isIntRegExp.matcher(height).matches() || NumberUtils.toInt(height) == 0) {
            addMetadataProperty(metadata, "height", naturalHeight);
        } else {
            addMetadataProperty(metadata, "height", height);
        }

        List<String> extraClasses = new ArrayList<>();

        for (String clazz : attrs.getClasses()) {
            if (Pattern.matches("^wp-image.*", clazz)) {
                String attachmentIdString = clazz.replace("wp-image-", "");
                if (NumberUtils.toInt(attachmentIdString) != 0) {
                    addMetadataProperty(metadata, "attachment_id", attachmentIdString);
                } else {
                    AppLog.d(AppLog.T.EDITOR, "AttachmentId was not an integer! String value: " + attachmentIdString);
                }
            } else if (Pattern.matches("^align.*", clazz)) {
                addMetadataProperty(metadata, "align", clazz.replace("align-", ""));
            } else if (Pattern.matches("^size-.*", clazz)) {
                addMetadataProperty(metadata, "size", clazz);
            } else {
                extraClasses.add(clazz);
            }
        }

        addMetadataProperty(metadata, "classes", TextUtils.join(" ", extraClasses));

        return metadata;
    }

    private static JSONObject addMetadataProperty(JSONObject jsonObject, String key, String value) {
        try {
            return jsonObject.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    private static JSONObject addMetadataProperty(JSONObject jsonObject, String key, int value) {
        try {
            return jsonObject.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    private static JSONObject addMetadataProperty(JSONObject jsonObject, String key, boolean value) {
        try {
            return jsonObject.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

    static Set<String> getClassAttribute(Attributes attributes) {
        if (attributes.getIndex("class") == -1) {
            return new HashSet<>(new ArrayList<String>());
        }
        return new HashSet<>(Arrays.asList(attributes.getValue("class").split(" ")));
    }

    static class AttributesWithClass {
        private AztecAttributes mAztecAttributes;
        private Set<String> mClasses;

        AttributesWithClass(Attributes attrs) {
            mAztecAttributes = new AztecAttributes(attrs);
            mClasses = getClassAttribute(attrs);
        }

        void addClass(String c) {
            mClasses.add(c);
        }

        void removeClassStartingWith(String prefix) {
            Iterator<String> iterator = mClasses.iterator();
            while (iterator.hasNext()) {
                String cls = iterator.next();
                if (cls.startsWith(prefix)) {
                    iterator.remove();
                }
            }
        }

        boolean hasClassStartingWith(String prefix) {
            for (String cls : mClasses) {
                if (cls.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        void removeClass(String c) {
            mClasses.remove(c);
        }

        boolean hasClass(String clazz) {
            return mClasses.contains(clazz);
        }

        public Set<String> getClasses() {
            return mClasses;
        }

        AztecAttributes getAttributes() {
            updateClassAttribute();
            return mAztecAttributes;
        }

        String getAttribute(String key, String defaultValue) {
            updateClassAttribute();

            if (mAztecAttributes.hasAttribute(key)) {
                return mAztecAttributes.getValue(key);
            } else {
                return defaultValue;
            }
        }

        private void updateClassAttribute() {
            String classesStr = TextUtils.join(" ", mClasses);
            mAztecAttributes.setValue("class", classesStr);
        }
    }
}
