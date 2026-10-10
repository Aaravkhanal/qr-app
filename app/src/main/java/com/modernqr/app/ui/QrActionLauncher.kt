package com.modernqr.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.modernqr.app.domain.qr.QrAction
import com.modernqr.app.domain.qr.WifiSecurity

fun launchQrAction(context: Context, action: QrAction) {
    val intent = when (action) {
        is QrAction.OpenUrl -> Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
        is QrAction.OpenMap -> Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:${action.latitude},${action.longitude}")
        )
        is QrAction.Dial -> Intent(
            Intent.ACTION_DIAL,
            Uri.Builder().scheme("tel").opaquePart(action.number).build()
        )
        is QrAction.SendSms -> Intent(
            Intent.ACTION_SENDTO,
            Uri.Builder().scheme("smsto").opaquePart(action.number).build()
        ).putExtra("sms_body", action.message)
        is QrAction.SendEmail -> Intent(
            Intent.ACTION_SENDTO,
            Uri.Builder().scheme("mailto").opaquePart(action.address).build()
        ).apply {
            putExtra(Intent.EXTRA_SUBJECT, action.subject)
            putExtra(Intent.EXTRA_TEXT, action.body)
        }
        is QrAction.AddContact -> Intent(Intent.ACTION_SEND).apply {
            type = "text/x-vcard"
            putExtra(Intent.EXTRA_TEXT, action.encodedVCard)
        }
        is QrAction.ConnectWifi -> wifiIntent(action)
        QrAction.None -> return
    }

    if (action is QrAction.AddContact) {
        context.startActivity(Intent.createChooser(intent, "Add Contact"))
    } else {
        context.startActivity(intent)
    }
}

fun QrAction.label(): String = when (this) {
    is QrAction.OpenUrl -> "Open URL"
    is QrAction.OpenMap -> "Open Map"
    is QrAction.ConnectWifi -> "Connect to Wi-Fi"
    is QrAction.AddContact -> "Add Contact"
    is QrAction.SendEmail -> "Email"
    is QrAction.Dial -> "Call $number"
    is QrAction.SendSms -> "Send SMS"
    QrAction.None -> ""
}

private fun wifiIntent(action: QrAction.ConnectWifi): Intent {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || action.security == WifiSecurity.WEP) {
        return Intent(Settings.ACTION_WIFI_SETTINGS)
    }

    val suggestion = android.net.wifi.WifiNetworkSuggestion.Builder()
        .setSsid(action.ssid)
        .apply {
            if (action.security == WifiSecurity.WPA && action.password.isNotEmpty()) {
                setWpa2Passphrase(action.password)
            }
        }
        .build()
    return Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
        putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
    }
}
