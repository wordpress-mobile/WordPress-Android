package org.wordpress.android.editor

import android.text.TextUtils
import org.apache.commons.lang3.math.NumberUtils
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.util.AppLog
import org.wordpress.aztec.AztecAttributes
import org.wordpress.aztec.plugins.shortcodes.extensions.ATTRIBUTE_VIDEOPRESS_HIDDEN_SRC
import org.xml.sax.Attributes
import java.util.Arrays
import java.util.regex.Pattern

object MetadataUtils {
    @JvmStatic
    @Suppress("LongMethod")
    fun getMetadata(attrs: AttributesWithClass, naturalWidth: Int, naturalHeight: Int): JSONObject {
        val metadata = JSONObject()
        addMetadataProperty(metadata, "align", "none") // Accepted values: center, left, right or empty string.
        addMetadataProperty(metadata, "alt", "") // Image alt attribute
        // Numeric attachment id of the image in the site's media library
        addMetadataProperty(metadata, "attachment_id", "")
        addMetadataProperty(metadata, "caption", "") // The text of the caption for the image (if any)
        addMetadataProperty(metadata, "captionClassName", "") // The classes for the caption shortcode (if any).
        // The caption shortcode's ID attribute. The numeric value should match the value of attachment_id
        addMetadataProperty(metadata, "captionId", "")
        // The class attribute for the image. Does not include editor generated classes
        addMetadataProperty(metadata, "classes", "")
        addMetadataProperty(metadata, "height", "") // The image height attribute
        addMetadataProperty(metadata, "linkClassName", "") // The class attribute for the link
        addMetadataProperty(metadata, "linkRel", "") // The rel attribute for the link (if any)
        addMetadataProperty(metadata, "linkTargetBlank", false) // true if the link should open in a new window.
        addMetadataProperty(metadata, "linkUrl", "") // The href attribute of the link
        // Accepted values: size-thumbnail, size-medium, size-large, size-full
        addMetadataProperty(metadata, "size", "size-full")
        addMetadataProperty(metadata, "src", "") // The src attribute of the image
        addMetadataProperty(metadata, "title", "") // The title attribute of the image (if any)
        addMetadataProperty(metadata, "width", "") // The image width attribute
        addMetadataProperty(metadata, "naturalWidth", "") // The natural width of the image.
        addMetadataProperty(metadata, "naturalHeight", "") // The natural height of the image.

        var src = attrs.getAttribute("src", "")
        if (TextUtils.isEmpty(src)) {
            src = attrs.getAttribute(ATTRIBUTE_VIDEOPRESS_HIDDEN_SRC, "")
        }
        addMetadataProperty(metadata, "src", src)

        addMetadataProperty(metadata, "alt", attrs.getAttribute("alt", ""))
        addMetadataProperty(metadata, "title", attrs.getAttribute("title", ""))
        addMetadataProperty(metadata, "naturalWidth", naturalWidth)
        addMetadataProperty(metadata, "naturalHeight", naturalHeight)

        val isIntRegExp = Pattern.compile("^\\d+$")

        val width = attrs.getAttribute("width", "")
        if (!isIntRegExp.matcher(width).matches() || NumberUtils.toInt(width) == 0) {
            addMetadataProperty(metadata, "width", naturalWidth)
        } else {
            addMetadataProperty(metadata, "width", width)
        }

        val height = attrs.getAttribute("height", "")
        if (!isIntRegExp.matcher(height).matches() || NumberUtils.toInt(height) == 0) {
            addMetadataProperty(metadata, "height", naturalHeight)
        } else {
            addMetadataProperty(metadata, "height", height)
        }

        val extraClasses: MutableList<String?> = ArrayList()

        for (clazz in attrs.classes) {
            if (Pattern.matches("^wp-image.*", clazz)) {
                val attachmentIdString = clazz.replace("wp-image-", "")
                if (NumberUtils.toInt(attachmentIdString) != 0) {
                    addMetadataProperty(metadata, "attachment_id", attachmentIdString)
                } else {
                    AppLog.d(AppLog.T.EDITOR, "AttachmentId was not an integer! String value: $attachmentIdString")
                }
            } else if (Pattern.matches("^align.*", clazz)) {
                addMetadataProperty(metadata, "align", clazz.replace("align-", ""))
            } else if (Pattern.matches("^size-.*", clazz)) {
                addMetadataProperty(metadata, "size", clazz)
            } else {
                extraClasses.add(clazz)
            }
        }

        addMetadataProperty(metadata, "classes", TextUtils.join(" ", extraClasses))

        return metadata
    }

    @Suppress("PrintStackTrace")
    private fun addMetadataProperty(jsonObject: JSONObject, key: String, value: String?): JSONObject {
        try {
            return jsonObject.put(key, value)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return jsonObject
    }

    @Suppress("PrintStackTrace")
    private fun addMetadataProperty(jsonObject: JSONObject, key: String, value: Int): JSONObject {
        try {
            return jsonObject.put(key, value)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return jsonObject
    }

    @Suppress("SameParameterValue", "PrintStackTrace")
    private fun addMetadataProperty(jsonObject: JSONObject, key: String, value: Boolean): JSONObject {
        try {
            return jsonObject.put(key, value)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return jsonObject
    }

    @JvmStatic
    @Suppress("SpreadOperator")
    fun getClassAttribute(attributes: Attributes): MutableSet<String> {
        if (attributes.getIndex("class") == -1) {
            return HashSet(ArrayList())
        }
        return HashSet(Arrays.asList(*attributes.getValue("class").split(" ").toTypedArray()))
    }

    class AttributesWithClass(attrs: Attributes) {
        private val mAztecAttributes: AztecAttributes
        private val mClasses: MutableSet<String>

        init {
            mAztecAttributes = AztecAttributes(attrs)
            mClasses = getClassAttribute(attrs)
        }

        fun addClass(c: String) {
            mClasses.add(c)
        }

        fun removeClassStartingWith(prefix: String) {
            val iterator = mClasses.iterator()
            while (iterator.hasNext()) {
                val cls = iterator.next()
                if (cls.startsWith(prefix)) {
                    iterator.remove()
                }
            }
        }

        fun hasClassStartingWith(prefix: String): Boolean {
            for (cls in mClasses) {
                if (cls.startsWith(prefix)) {
                    return true
                }
            }
            return false
        }

        fun removeClass(c: String?) {
            mClasses.remove(c)
        }

        fun hasClass(clazz: String?): Boolean {
            return mClasses.contains(clazz)
        }

        val classes: Set<String>
            get() = mClasses

        val attributes: AztecAttributes
            get() {
                updateClassAttribute()
                return mAztecAttributes
            }

        fun getAttribute(key: String, defaultValue: String): String {
            updateClassAttribute()

            return if (mAztecAttributes.hasAttribute(key)) {
                mAztecAttributes.getValue(key)
            } else {
                defaultValue
            }
        }

        private fun updateClassAttribute() {
            val classesStr = TextUtils.join(" ", mClasses)
            mAztecAttributes.setValue("class", classesStr)
        }
    }
}
