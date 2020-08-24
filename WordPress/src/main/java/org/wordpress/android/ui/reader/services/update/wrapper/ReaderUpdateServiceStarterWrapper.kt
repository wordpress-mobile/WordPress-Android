package org.wordpress.android.ui.reader.services.update.wrapper

import android.content.Context
import dagger.Reusable
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter
import java.util.EnumSet
import javax.inject.Inject

@Reusable
class ReaderUpdateServiceStarterWrapper @Inject constructor() {
    fun startService(context: Context, tasks: EnumSet<ReaderUpdateLogic.UpdateTask>) =
            ReaderUpdateServiceStarter.startService(context, tasks)
}
