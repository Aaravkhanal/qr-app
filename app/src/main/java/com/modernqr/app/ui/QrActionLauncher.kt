package com.modernqr.app.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import com.modernqr.app.domain.qr.QrAction
import com.modernqr.app.domain.qr.WifiSecurity

fun launchQrAction(context: Context, action: QrAction) {
    val prepared = when (action) {
        is QrAction.OpenUrl -> prepareWebUrl(action.url)
        is QrAction.OpenMap -> PreparedAction(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:${action.latitude},${action.longitude}")),
            unavailableMessage = "No maps application is available."
        )
        is QrAction.Dial -> PreparedAction(
            Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", action.number, null)),
            unavailableMessage = "No phone application is available."
        )
        is QrAction.SendSms -> PreparedAction(
            Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", action.number, null)).apply {
                putExtra("sms_body", action.message)
            },
            unavailableMessage = "No messaging application is available."
        )
        is QrAction.SendEmail -> prepareEmail(action)
        is QrAction.AddContact -> prepareContact(action)
        is QrAction.AddWifiNetwork -> prepareWifi(action)
        QrAction.None -> return
    }

    if (prepared.intent == null) {
        showMessage(context, prepared.explanation ?: prepared.unavailableMessage)
        return
    }

    if (prepared.intent.resolveActivity(context.packageManager) == null) {
        showMessage(context, prepared.unavailableMessage)
        return
    }

    try {
        context.startActivity(prepared.intent)
        prepared.explanation?.let { showMessage(context, it) }
    } catch (_: RuntimeException) {
        showMessage(context, "This action could not be opened. Please try again from another app.")
    }
}

fun QrAction.label(): String = when (this) {
    is QrAction.OpenUrl -> "Open URL"
    is QrAction.OpenMap -> "Open Map"
    is QrAction.AddWifiNetwork -> "Add Wi-Fi network"
    is QrAction.AddContact -> "Create Contact"
    is QrAction.SendEmail -> "Email"
    is QrAction.Dial -> "Call $number"
    is QrAction.SendSms -> "Send SMS"
    QrAction.None -> ""
}

private fun prepareWebUrl(value: String): PreparedAction {
    val uri = runCatching { Uri.parse(value) }.getOrNull()
    val safeScheme = uri?.scheme?.lowercase() in setOf("http", "https")
    return if (uri != null && safeScheme && !uri.host.isNullOrBlank()) {
        PreparedAction(
            Intent(Intent.ACTION_VIEW, uri),
            unavailableMessage = "No browser is available to open this URL."
        )
    } else {
        PreparedAction(
            intent = null,
            unavailableMessage = "This link cannot be opened.",
            explanation = "Only valid HTTP and HTTPS links can be opened."
        )
    }
}

private fun prepareEmail(action: QrAction.SendEmail): PreparedAction {
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
        unavailableMessage = "No email application is available."
    )
}

private fun prepareContact(action: QrAction.AddContact): PreparedAction {
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
    return PreparedAction(intent, unavailableMessage = "No contacts application is available.")
}

private fun prepareWifi(action: QrAction.AddWifiNetwork): PreparedAction {
    if (action.security == WifiSecurity.UNSUPPORTED) {
        return wifiSettings(
            "This Wi-Fi security type is not supported by the app. Add the network manually in Wi-Fi settings."
        )
    }
    if (action.security == WifiSecurity.WEP) {
        return wifiSettings(
            "Android cannot add WEP networks through the secure confirmation flow. Add this network manually in Wi-Fi settings."
        )
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return wifiSettings(
            "On Android 10 and earlier, this network must be added manually in Wi-Fi settings."
        )
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
            unavailableMessage = "The system Wi-Fi network screen is not available.",
            explanation = "Review and confirm the network in the Android system screen."
        )
    } catch (_: IllegalArgumentException) {
        wifiSettings("Android rejected this network configuration. Add it manually in Wi-Fi settings.")
    }
}

private fun wifiSettings(explanation: String) = PreparedAction(
    Intent(Settings.ACTION_WIFI_SETTINGS),
    unavailableMessage = "Wi-Fi settings are not available on this device.",
    explanation = explanation
)

private fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

private data class PreparedAction(
    val intent: Intent?,
    val unavailableMessage: String,
    val explanation: String? = null
)
