package org.wordpress.android.ui.photopicker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.Video
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import org.wordpress.android.util.SqlUtils

/*
 * builds the list of media items from the device
 */
class BuildDeviceMediaListTask(
    private val mBrowserType: MediaBrowserType,
    private val mContext: Context,
    private val mListener: BuildDeviceMediaListListener
) : AsyncTask<Void, Void, List<PhotoPickerItem>>() {
    override fun doInBackground(vararg params: Void): List<PhotoPickerItem> {
        val result = mutableListOf<PhotoPickerItem>()
        // images
        if (mBrowserType.isImagePicker) {
            result.addAll(addMedia(Media.EXTERNAL_CONTENT_URI, false))
        }

        // videos
        if (mBrowserType.isVideoPicker) {
            result.addAll(addMedia(Video.Media.EXTERNAL_CONTENT_URI, true))
        }
        result.sortByDescending { it.id }
        return result
    }

    private fun addMedia(baseUri: Uri, isVideo: Boolean): List<PhotoPickerItem> {
        val projection = arrayOf(ID_COL)
        var cursor: Cursor? = null
        val result = mutableListOf<PhotoPickerItem>()
        try {
            cursor = mContext.contentResolver.query(
                    baseUri,
                    projection,
                    null,
                    null,
                    null
            )
        } catch (e: SecurityException) {
            AppLog.e(MEDIA, e)
        }
        if (cursor == null) {
            return result
        }
        try {
            val idIndex = cursor.getColumnIndexOrThrow(ID_COL)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val item = PhotoPickerItem(
                        id,
                        Uri.withAppendedPath(baseUri, "" + id),
                        isVideo
                )
                result.add(item)
            }
        } finally {
            SqlUtils.closeCursor(cursor)
        }
        return result
    }

    override fun onCancelled() {
        super.onCancelled()
        mListener.onCancelled()
    }

    override fun onPostExecute(result: List<PhotoPickerItem>) {
        mListener.onSuccess(result)
    }

    interface BuildDeviceMediaListListener {
        fun onCancelled()
        fun onSuccess(result: List<PhotoPickerItem>)
    }

    companion object {
        private const val ID_COL = Media._ID
    }
}
