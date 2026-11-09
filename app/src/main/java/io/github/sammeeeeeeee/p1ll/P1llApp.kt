package io.github.sammeeeeeeee.p1ll

import android.app.Application
import android.content.Context
import io.github.sammeeeeeeee.p1ll.data.P1llDb
import io.github.sammeeeeeeee.p1ll.data.SettingsStore
import io.github.sammeeeeeeee.p1ll.data.Store
import io.github.sammeeeeeeee.p1ll.reminders.Notifications
import io.github.sammeeeeeeee.p1ll.ui.feedback.Feedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class Container(context: Context) {
    val db = P1llDb.create(context)
    val settings = SettingsStore(context)
    val store = Store(context, db, settings)
    val feedback by lazy { Feedback(context) }
    /** For writes that must finish after the screen that started them has closed. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

class P1llApp : Application() {
    lateinit var container: Container
        private set

    override fun onCreate() {
        super.onCreate()
        container = Container(this)
        Notifications.ensureChannels(this)
    }
}
