package org.wordpress.android.workers

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.wordpress.android.R
import org.wordpress.android.workers.LocalNotification.Type

class LocalNotificationWorker(
    val context: Context,
    params: WorkerParameters,
    private val localNotificationHandlerFactory: LocalNotificationHandlerFactory
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getInt(ID, -1)
        val type = Type.fromTag(inputData.getString(TYPE))

        if (id == -1 || type == null) return Result.failure()

        val localNotificationHandler = localNotificationHandlerFactory.buildLocalNotificationHandler(type)
        if (localNotificationHandler.shouldShowNotification()) {
            NotificationManagerCompat.from(context).notify(id, localNotificationBuilder().build())
        }

        return Result.success()
    }

    private fun localNotificationBuilder(): NotificationCompat.Builder {
        val title = inputData.getInt(TITLE, -1)
        val text = inputData.getInt(TEXT, -1)
        val icon = inputData.getInt(ICON, -1)
        val actionIcon = inputData.getInt(ACTION_ICON, -1)
        val actionTitle = inputData.getInt(ACTION_TITLE, -1)

        return NotificationCompat.Builder(context, context.getString(R.string.notification_channel_normal_id)).apply {
            val pendingIntent = getPendingIntent()
            setContentIntent(pendingIntent)
            setSmallIcon(icon)
            setContentTitle(context.getString(title))
            setContentText(context.getString(text))
            addAction(actionIcon, context.getString(actionTitle), pendingIntent)
            priority = NotificationCompat.PRIORITY_DEFAULT
            setCategory(NotificationCompat.CATEGORY_REMINDER)
            setAutoCancel(true)
            setColorized(true)
            color = ContextCompat.getColor(context, R.color.blue_50)
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val type = Type.fromTag(inputData.getString(TYPE))
        val handler = type?.let { localNotificationHandlerFactory.buildLocalNotificationHandler(it) }
        return PendingIntent.getActivity(
                context,
                0,
                handler?.buildIntent(context),
                PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    class Factory(
        private val localNotificationHandlerFactory: LocalNotificationHandlerFactory
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return if (workerClassName == LocalNotificationWorker::class.java.name) {
                LocalNotificationWorker(appContext, workerParameters, localNotificationHandlerFactory)
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
        private const val ACTION_ICON = "key_action_icon"
        private const val ACTION_TITLE = "key_action_title"

        fun buildData(localNotification: LocalNotification): Data {
            return workDataOf(
                    TYPE to localNotification.type.tag,
                    ID to localNotification.id,
                    TITLE to localNotification.title,
                    TEXT to localNotification.text,
                    ICON to localNotification.icon,
                    ACTION_ICON to localNotification.actionIcon,
                    ACTION_TITLE to localNotification.actionTitle
            )
        }
    }
}
