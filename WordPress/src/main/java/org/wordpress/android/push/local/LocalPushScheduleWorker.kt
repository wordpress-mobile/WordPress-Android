package org.wordpress.android.push.local

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.wordpress.android.R
import org.wordpress.android.push.local.LocalPush.Type

class LocalPushScheduleWorker(
    val context: Context,
    workerParams: WorkerParameters,
    private val localPushHandlerFactory: LocalPushHandlerFactory
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val type = Type.fromTag(inputData.getString(TYPE)) ?: return Result.failure()
            val handler = localPushHandlerFactory.buildLocalPushHandler(type)
            if (!handler.shouldShowNotification()) {
                return Result.failure()
            }
            val id = inputData.getInt(ID, -1)
            val title = inputData.getInt(TITLE, -1)
            val text = inputData.getInt(TEXT, -1)
            val icon = inputData.getInt(ICON, -1)
            if (id == -1 || title == -1 || text == -1) {
                return Result.failure()
            }
            val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    handler.buildIntent(context),
                    PendingIntent.FLAG_CANCEL_CURRENT
            )
            val builder = NotificationCompat.Builder(
                    context,
                    context.getString(R.string.notification_channel_normal_id)
            )
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(icon)
                    .setContentTitle(context.getString(title))
                    .setContentText(context.getString(text))
                    .addAction(R.drawable.ic_story_icon_24dp, context.getString(R.string.cancel),
                            pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setColorized(true)
                    .setColor(ContextCompat.getColor(context, R.color.blue_50))
                    .setLargeIcon(ContextCompat.getDrawable(context, icon)?.toBitmap(100, 100))

            with(NotificationManagerCompat.from(context)) {
                notify(id, builder.build())
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    class Factory(
        private val localPushHandlerFactory: LocalPushHandlerFactory
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return if (workerClassName == LocalPushScheduleWorker::class.java.name) {
                LocalPushScheduleWorker(appContext, workerParameters, localPushHandlerFactory)
            } else {
                null
            }
        }
    }

    companion object {
        private const val TYPE = "key_type"
        private const val ID = "key_id"
        private const val TITLE = "key_title"
        private const val TEXT = "key_text"
        private const val ICON = "key_icon"
        fun buildData(localNotification: LocalPush): Data {
            return workDataOf(
                    TYPE to localNotification.type.tag,
                    ID to localNotification.id,
                    TITLE to localNotification.title,
                    TEXT to localNotification.text,
                    ICON to localNotification.icon
            )
        }
    }
}
