package com.ritikqrscanner.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
import com.ritikqrscanner.app.MainActivity
import com.ritikqrscanner.app.R
import com.ritikqrscanner.app.data.HistoryPrivacyPolicy
import com.ritikqrscanner.app.studio.QrStudio
import java.io.File

class QrWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> update(context, manager, id) }
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.ritikqrscanner.app.PIN_WIDGET") {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val token = intent.getStringExtra("token") ?: return
            if (id < 0 || !token.matches(Regex("[a-f0-9-]+"))) return
            val source = File(context.filesDir, "widget-$token.png")
            if (source.exists()) source.renameTo(File(context.filesDir, "widget-$id.png"))
            context.getSharedPreferences("widgets", 0).edit().putString("title-$id", intent.getStringExtra("title")).apply()
            update(context, AppWidgetManager.getInstance(context), id)
        }
    }
    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { File(context.filesDir, "widget-$it.png").delete(); context.getSharedPreferences("widgets", 0).edit().remove("title-$it").apply() }
    }
    companion object {
        suspend fun pin(context: Context, content: String, title: String) {
            if (!HistoryPrivacyPolicy.canStore(content, "QR_CODE")) return
            if (Build.VERSION.SDK_INT < 26) { Toast.makeText(context, "Pinning widgets requires Android 8 or newer", Toast.LENGTH_LONG).show(); return }
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) { Toast.makeText(context, "Your launcher does not support widget pinning", Toast.LENGTH_LONG).show(); return }
            try {
                val token = java.util.UUID.randomUUID().toString()
                val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    val original = QrStudio.render(content, android.graphics.Color.BLACK, android.graphics.Color.WHITE, false, null, "QR only", title)
                    val scaled = android.graphics.Bitmap.createScaledBitmap(original, 320, 320, false)
                    if (original !== scaled) original.recycle()
                    QrStudio.verify(scaled, content)
                    scaled
                }
                File(context.filesDir, "widget-$token.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                val callback = PendingIntent.getBroadcast(context, token.hashCode(), Intent(context, QrWidget::class.java).setAction("com.ritikqrscanner.app.PIN_WIDGET").putExtra("token", token).putExtra("title", title), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                val preview = RemoteViews(context.packageName, R.layout.qr_widget).apply {
                    setTextViewText(R.id.widget_title, title)
                    setImageViewBitmap(R.id.widget_qr, bitmap)
                }
                val options = Bundle().apply { putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview) }
                val requested = manager.requestPinAppWidget(ComponentName(context, QrWidget::class.java), options, callback)
                bitmap.recycle()
                if (!requested) {
                    File(context.filesDir, "widget-$token.png").delete()
                    Toast.makeText(context, "Your launcher could not pin the widget", Toast.LENGTH_LONG).show()
                }
            } catch (_: Exception) { Toast.makeText(context, "Could not create a widget for this code", Toast.LENGTH_LONG).show() }
        }
        private fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.qr_widget)
            views.setTextViewText(R.id.widget_title, context.getSharedPreferences("widgets", 0).getString("title-$id", "Choose a code in History"))
            val file = File(context.filesDir, "widget-$id.png")
            if (file.exists()) views.setImageViewBitmap(R.id.widget_qr, android.graphics.BitmapFactory.decodeFile(file.path))
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id, views)
        }
    }
}
