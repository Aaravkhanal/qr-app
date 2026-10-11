package com.ritikqrscanner.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ritikqrscanner.app.R
import com.ritikqrscanner.app.domain.qr.QrPayloadType

@Composable
internal fun payloadTypeLabel(type: QrPayloadType): String = stringResource(
    when (type) {
        QrPayloadType.PLAIN_TEXT -> R.string.payload_type_text
        QrPayloadType.URL -> R.string.payload_type_url
        QrPayloadType.WIFI -> R.string.payload_type_wifi
        QrPayloadType.GEO -> R.string.payload_type_location
        QrPayloadType.VCARD -> R.string.payload_type_contact
        QrPayloadType.EMAIL -> R.string.payload_type_email
        QrPayloadType.TELEPHONE -> R.string.payload_type_phone
        QrPayloadType.SMS -> R.string.payload_type_sms
        QrPayloadType.UNKNOWN -> R.string.payload_type_unknown
    }
)
