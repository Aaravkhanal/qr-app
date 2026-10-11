package com.ritikqrscanner.app.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import com.ritikqrscanner.app.domain.qr.QrAction
import com.ritikqrscanner.app.domain.qr.WifiSecurity
import com.ritikqrscanner.app.R

fun launchQrAction(context: Context, action: QrAction): String? =
    launchQrAction(context, action, AndroidQrIntentDispatcher(context))

internal fun launchQrAction(
    context: Context,
    action: QrAction,
    dispatcher: QrIntentDispatcher
): String? {
    val prepared = when (action) {
        is QrAction.OpenUrl -> prepareWebUrl(context, action.url)
        is QrAction.OpenMap -> PreparedAction(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:${action.latitude},${action.longitude}")),
            unavailableMessage = context.getString(R.string.no_maps_app)
        )
        is QrAction.Dial -> PreparedAction(
            Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", action.number, null)),
            unavailableMessage = context.getString(R.string.no_phone_app)
        )
        is QrAction.SendSms -> PreparedAction(
            Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", action.number, null)).apply {
                putExtra("sms_body", action.message)
            },
            unavailableMessage = context.getString(R.string.no_messaging_app)
        )
        is QrAction.SendEmail -> prepareEmail(context, action)
        is QrAction.AddContact -> prepareContact(context, action)
        is QrAction.AddWifiNetwork -> prepareWifi(context, action)
        QrAction.None -> return null
    }

    if (prepared.intent == null) {
        return prepared.explanation ?: prepared.unavailableMessage
    }

    if (!dispatcher.canResolve(prepared.intent)) {
        return prepared.unavailableMessage
    }

    return try {
        dispatcher.launch(prepared.intent)
        prepared.explanation
    } catch (_: RuntimeException) {
        context.getString(R.string.action_open_failed)
    }
}

internal interface QrIntentDispatcher {
    fun canResolve(intent: Intent): Boolean
    fun launch(intent: Intent)
}

private class AndroidQrIntentDispatcher(private val context: Context) : QrIntentDispatcher {
    override fun canResolve(intent: Intent): Boolean = intent.resolveActivity(context.packageManager) != null
    override fun launch(intent: Intent) = context.startActivity(intent)
}

fun QrAction.label(context: Context): String = when (this) {
    is QrAction.OpenUrl -> context.getString(R.string.action_open_url)
    is QrAction.OpenMap -> context.getString(R.string.action_open_map)
    is QrAction.AddWifiNetwork -> context.getString(R.string.action_add_wifi)
    is QrAction.AddContact -> context.getString(R.string.action_create_contact)
    is QrAction.SendEmail -> context.getString(R.string.action_email)
    is QrAction.Dial -> context.getString(R.string.action_call, number)
    is QrAction.SendSms -> context.getString(R.string.action_send_sms)
    QrAction.None -> ""
}

private fun prepareWebUrl(context: Context, value: String): PreparedAction {
    val uri = runCatching { Uri.parse(value) }.getOrNull()
    val safeScheme = uri?.scheme?.lowercase() in setOf("http", "https")
    return if (uri != null && safeScheme && !uri.host.isNullOrBlank()) {
        PreparedAction(
            Intent(Intent.ACTION_VIEW, uri),
            unavailableMessage = context.getString(R.string.no_browser_app)
        )
    } else {
        PreparedAction(
            intent = null,
            unavailableMessage = context.getString(R.string.link_invalid),
            explanation = context.getString(R.string.link_safe_schemes)
        )
    }
}

private fun prepareEmail(context: Context, action: QrAction.SendEmail): PreparedAction {
    val query = buildList {
        if (action.subject.isNotEmpty()) add("subject=${Uri.encode(action.subject)}")
        if (action.body.isNotEmpty()) add("body=${Uri.encode(action.body)}")
    }.joinToString("&")
    val encodedRecipients = action.recipients.joinToString(",") { Uri.encode(it, "@") }
    val data = Uri.parse("mailto:$encodedRecipients${if (query.isEmpty()) "" else "?$query"}")
    return PreparedAction(
        Intent(Intent.ACTION_SENDTO, data).apply {
            putExtra(Intent.EXTRA_EMAIL, action.recipients.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, action.subject)
            putExtra(Intent.EXTRA_TEXT, action.body)
        },
        unavailableMessage = context.getString(R.string.no_email_app)
    )
}

private fun prepareContact(context: Context, action: QrAction.AddContact): PreparedAction {
    val websiteRows = if (action.url.isBlank()) {
        null
    } else {
        arrayListOf(ContentValues().apply {
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.Website.URL, action.url)
        })
    }
    val intent = Intent(Intent.ACTION_INSERT).apply {
        type = ContactsContract.Contacts.CONTENT_TYPE
        putExtra(ContactsContract.Intents.Insert.NAME, action.name)
        if (action.phone.isNotEmpty()) putExtra(ContactsContract.Intents.Insert.PHONE, action.phone)
        if (action.email.isNotEmpty()) putExtra(ContactsContract.Intents.Insert.EMAIL, action.email)
        if (action.note.isNotEmpty()) putExtra(ContactsContract.Intents.Insert.NOTES, action.note)
        if (websiteRows != null) putParcelableArrayListExtra(ContactsContract.Intents.Insert.DATA, websiteRows)
    }
    return PreparedAction(intent, unavailableMessage = context.getString(R.string.no_contacts_app))
}

private fun prepareWifi(context: Context, action: QrAction.AddWifiNetwork): PreparedAction {
    if (action.security == WifiSecurity.UNSUPPORTED) {
        return wifiSettings(context, context.getString(R.string.wifi_unsupported_security))
    }
    if (action.security == WifiSecurity.WEP) {
        return wifiSettings(context, context.getString(R.string.wifi_wep_manual))
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return wifiSettings(context, context.getString(R.string.wifi_legacy_manual))
    }

    return try {
        val suggestion = android.net.wifi.WifiNetworkSuggestion.Builder()
            .setSsid(action.ssid)
            .setIsHiddenSsid(action.hidden)
            .apply {
                when (action.security) {
                    WifiSecurity.WPA2 -> if (action.password.isNotEmpty()) setWpa2Passphrase(action.password)
                    WifiSecurity.WPA3 -> if (action.password.isNotEmpty()) setWpa3Passphrase(action.password)
                    WifiSecurity.NONE -> Unit
                    WifiSecurity.WEP -> Unit
                    WifiSecurity.UNSUPPORTED -> Unit
                }
            }
            .build()
        PreparedAction(
            Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
                putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
            },
            unavailableMessage = context.getString(R.string.wifi_add_unavailable),
            explanation = context.getString(R.string.wifi_confirm)
        )
    } catch (_: IllegalArgumentException) {
        wifiSettings(context, context.getString(R.string.wifi_rejected))
    }
}

private fun wifiSettings(context: Context, explanation: String) = PreparedAction(
    Intent(Settings.ACTION_WIFI_SETTINGS),
    unavailableMessage = context.getString(R.string.wifi_settings_unavailable),
    explanation = explanation
)

private data class PreparedAction(
    val intent: Intent?,
    val unavailableMessage: String,
    val explanation: String? = null
)
