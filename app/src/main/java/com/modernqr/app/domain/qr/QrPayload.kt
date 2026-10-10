package com.modernqr.app.domain.qr

enum class QrPayloadType(val displayName: String) {
    PLAIN_TEXT("Text"),
    URL("URL"),
    WIFI("Wi-Fi"),
    GEO("Location"),
    VCARD("Contact"),
    EMAIL("Email"),
    TELEPHONE("Phone"),
    SMS("SMS"),
    UNKNOWN("Unknown barcode")
}

sealed interface QrPayload {
    val type: QrPayloadType

    data class PlainText(val text: String) : QrPayload {
        override val type = QrPayloadType.PLAIN_TEXT
    }

    data class Url(val url: String) : QrPayload {
        override val type = QrPayloadType.URL
    }

    data class Wifi(
        val ssid: String,
        val password: String = "",
        val security: WifiSecurity = WifiSecurity.WPA2,
        val hidden: Boolean = false
    ) : QrPayload {
        override val type = QrPayloadType.WIFI
    }

    data class Geo(val latitude: Double, val longitude: Double) : QrPayload {
        override val type = QrPayloadType.GEO
    }

    data class VCard(
        val name: String,
        val phone: String = "",
        val email: String = "",
        val url: String = "",
        val note: String = ""
    ) : QrPayload {
        override val type = QrPayloadType.VCARD
    }

    data class Email(
        val address: String,
        val subject: String = "",
        val body: String = ""
    ) : QrPayload {
        override val type = QrPayloadType.EMAIL

        val recipients: List<String>
            get() = address.split(',', ';').map(String::trim).filter(String::isNotEmpty)
    }

    data class Telephone(val number: String) : QrPayload {
        override val type = QrPayloadType.TELEPHONE
    }

    data class Sms(val number: String, val message: String = "") : QrPayload {
        override val type = QrPayloadType.SMS
    }

    /** A non-QR barcode whose content does not map to a supported QR payload. */
    data class Unknown(val rawValue: String, val barcodeFormat: String) : QrPayload {
        override val type = QrPayloadType.UNKNOWN
    }
}

enum class WifiSecurity(val wireValue: String) {
    WPA2("WPA"),
    WPA3("SAE"),
    WEP("WEP"),
    NONE("nopass"),
    UNSUPPORTED("unknown");

    companion object {
        fun fromWireValue(value: String): WifiSecurity? = when {
            value.equals("WPA", true) || value.equals("WPA2", true) -> WPA2
            value.equals("WPA3", true) || value.equals("SAE", true) -> WPA3
            value.equals("WEP", true) -> WEP
            value.equals("nopass", true) || value.equals("NONE", true) -> NONE
            else -> null
        }
    }
}

data class QrValidationResult(val errors: List<String>) {
    val isValid: Boolean get() = errors.isEmpty()
    val message: String? get() = errors.firstOrNull()
}

sealed interface QrAction {
    data class OpenUrl(val url: String) : QrAction
    data class OpenMap(val latitude: Double, val longitude: Double) : QrAction
    data class AddWifiNetwork(
        val ssid: String,
        val password: String,
        val security: WifiSecurity,
        val hidden: Boolean
    ) : QrAction
    data class AddContact(
        val name: String,
        val phone: String,
        val email: String,
        val url: String,
        val note: String
    ) : QrAction
    data class SendEmail(val recipients: List<String>, val subject: String, val body: String) : QrAction
    data class Dial(val number: String) : QrAction
    data class SendSms(val number: String, val message: String) : QrAction
    data object None : QrAction
}
