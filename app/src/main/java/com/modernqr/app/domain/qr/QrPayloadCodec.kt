package com.modernqr.app.domain.qr

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object QrPayloadEncoder {
    fun encode(payload: QrPayload): String = when (payload) {
        is QrPayload.PlainText -> payload.text
        is QrPayload.Url -> normalizeUrl(payload.url)
        is QrPayload.Wifi -> buildString {
            append("WIFI:T:").append(escape(payload.security.wireValue))
            append(";S:").append(escape(payload.ssid))
            if (payload.security != WifiSecurity.NONE || payload.password.isNotEmpty()) {
                append(";P:").append(escape(payload.password))
            }
            if (payload.hidden) append(";H:true")
            append(";;")
        }
        is QrPayload.Geo -> "geo:${formatCoordinate(payload.latitude)},${formatCoordinate(payload.longitude)}"
        is QrPayload.VCard -> buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            appendLine("FN:${escape(payload.name)}")
            appendLine("N:${escape(payload.name)}")
            if (payload.phone.isNotEmpty()) appendLine("TEL:${escape(payload.phone)}")
            if (payload.email.isNotEmpty()) appendLine("EMAIL:${escape(payload.email)}")
            if (payload.url.isNotEmpty()) appendLine("URL:${escape(payload.url)}")
            if (payload.note.isNotEmpty()) appendLine("NOTE:${escape(payload.note)}")
            append("END:VCARD")
        }
        is QrPayload.Email -> buildString {
            append("MATMSG:TO:").append(escape(payload.address))
            append(";SUB:").append(escape(payload.subject))
            append(";BODY:").append(escape(payload.body)).append(";;")
        }
        is QrPayload.Telephone -> "tel:${payload.number}"
        is QrPayload.Sms -> "smsto:${payload.number}:${escape(payload.message)}"
        is QrPayload.Unknown -> payload.rawValue
    }

    fun normalizeUrl(value: String): String {
        val trimmed = value.trim()
        return if (SCHEME_REGEX.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
    }

    internal fun escape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                ';' -> append("\\;")
                ':' -> append("\\:")
                ',' -> append("\\,")
                '\n' -> append("\\n")
                '\r' -> Unit
                else -> append(character)
            }
        }
    }

    private fun formatCoordinate(value: Double): String = value.toString().removeSuffix(".0")

    private val SCHEME_REGEX = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
}

object QrPayloadParser {
    fun parse(rawValue: String, barcodeFormat: String = "QR_CODE"): QrPayload {
        val candidate = rawValue.trimStart()
        val trimmed = candidate.trimEnd()
        val parsed = when {
            candidate.startsWith("WIFI:", ignoreCase = true) -> parseWifi(candidate)
            candidate.startsWith("geo:", ignoreCase = true) -> parseGeo(candidate)
            candidate.startsWith("BEGIN:VCARD", ignoreCase = true) -> parseVCard(candidate)
            candidate.startsWith("MATMSG:", ignoreCase = true) -> parseMatMsg(candidate)
            candidate.startsWith("mailto:", ignoreCase = true) -> parseMailTo(candidate)
            candidate.startsWith("tel:", ignoreCase = true) -> QrPayload.Telephone(candidate.substringAfter(':'))
            candidate.startsWith("smsto:", ignoreCase = true) || candidate.startsWith("sms:", ignoreCase = true) -> parseSms(candidate)
            isHttpUrl(trimmed) || looksLikeWebAddress(trimmed) -> QrPayload.Url(QrPayloadEncoder.normalizeUrl(trimmed))
            looksLikeEmail(trimmed) -> QrPayload.Email(trimmed)
            else -> null
        }

        return parsed ?: if (barcodeFormat.equals("QR_CODE", ignoreCase = true)) {
            QrPayload.PlainText(rawValue)
        } else {
            QrPayload.Unknown(rawValue, barcodeFormat)
        }
    }

    private fun parseWifi(value: String): QrPayload.Wifi {
        val fields = parseDelimitedFields(value.substringAfter(':'), ';')
        return QrPayload.Wifi(
            ssid = fields["S"].orEmpty(),
            password = fields["P"].orEmpty(),
            security = WifiSecurity.fromWireValue(fields["T"].orEmpty()) ?: WifiSecurity.WPA,
            hidden = fields["H"].equals("true", ignoreCase = true)
        )
    }

    private fun parseGeo(value: String): QrPayload {
        val coordinates = value.substringAfter(':').substringBefore('?').split(',', limit = 2)
        val latitude = coordinates.getOrNull(0)?.trim()?.toDoubleOrNull()
        val longitude = coordinates.getOrNull(1)?.trim()?.toDoubleOrNull()
        return QrPayload.Geo(latitude ?: Double.NaN, longitude ?: Double.NaN)
    }

    private fun parseVCard(value: String): QrPayload.VCard {
        val fields = mutableMapOf<String, String>()
        value.lineSequence().drop(1).forEach { line ->
            val separator = indexOfUnescaped(line, ':')
            if (separator > 0) {
                val key = line.substring(0, separator).substringBefore(';').uppercase()
                if (key !in fields) fields[key] = unescape(line.substring(separator + 1))
            }
        }
        return QrPayload.VCard(
            name = fields["FN"] ?: fields["N"].orEmpty(),
            phone = fields["TEL"].orEmpty(),
            email = fields["EMAIL"].orEmpty(),
            url = fields["URL"].orEmpty(),
            note = fields["NOTE"].orEmpty()
        )
    }

    private fun parseMatMsg(value: String): QrPayload.Email {
        val fields = parseDelimitedFields(value.substringAfter(':'), ';')
        return QrPayload.Email(
            address = fields["TO"].orEmpty(),
            subject = fields["SUB"].orEmpty(),
            body = fields["BODY"].orEmpty()
        )
    }

    private fun parseMailTo(value: String): QrPayload.Email {
        val withoutScheme = value.substringAfter(':')
        val address = decodeUrlPath(withoutScheme.substringBefore('?'))
        val query = withoutScheme.substringAfter('?', "")
            .split('&')
            .filter { it.isNotEmpty() }
            .associate { part ->
                decodeUrl(part.substringBefore('=')).lowercase() to decodeUrl(part.substringAfter('=', ""))
            }
        return QrPayload.Email(address, query["subject"].orEmpty(), query["body"].orEmpty())
    }

    private fun parseSms(value: String): QrPayload.Sms {
        val remainder = value.substringAfter(':')
        val separator = indexOfUnescaped(remainder, ':')
        return if (separator < 0) {
            QrPayload.Sms(remainder)
        } else {
            QrPayload.Sms(remainder.substring(0, separator), unescape(remainder.substring(separator + 1)))
        }
    }

    private fun parseDelimitedFields(value: String, delimiter: Char): Map<String, String> {
        val result = linkedMapOf<String, String>()
        splitUnescaped(value, delimiter).forEach { field ->
            val separator = indexOfUnescaped(field, ':')
            if (separator > 0) {
                result[field.substring(0, separator).uppercase()] = unescape(field.substring(separator + 1))
            }
        }
        return result
    }

    private fun splitUnescaped(value: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        var escaped = false
        value.forEachIndexed { index, character ->
            when {
                escaped -> escaped = false
                character == '\\' -> escaped = true
                character == delimiter -> {
                    result += value.substring(start, index)
                    start = index + 1
                }
            }
        }
        result += value.substring(start)
        return result
    }

    private fun indexOfUnescaped(value: String, target: Char): Int {
        var escaped = false
        value.forEachIndexed { index, character ->
            when {
                escaped -> escaped = false
                character == '\\' -> escaped = true
                character == target -> return index
            }
        }
        return -1
    }

    internal fun unescape(value: String): String = buildString(value.length) {
        var index = 0
        while (index < value.length) {
            if (value[index] == '\\' && index + 1 < value.length) {
                when (val next = value[index + 1]) {
                    'n', 'N' -> append('\n')
                    '\\', ';', ':', ',' -> append(next)
                    else -> {
                        append('\\')
                        append(next)
                    }
                }
                index += 2
            } else {
                append(value[index++])
            }
        }
    }

    private fun isHttpUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)

    private fun looksLikeWebAddress(value: String): Boolean =
        value.none { it == ' ' || it == '\n' || it == '\r' } &&
            WEB_ADDRESS_REGEX.matches(value) &&
            !value.contains('@')

    private fun looksLikeEmail(value: String): Boolean = EMAIL_REGEX.matches(value)

    private fun decodeUrl(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun decodeUrlPath(value: String): String = decodeUrl(value.replace("+", "%2B"))

    private val WEB_ADDRESS_REGEX = Regex("(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}(?::\\d+)?(?:/\\S*)?")
    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
}

object QrPayloadValidator {
    fun validate(payload: QrPayload): QrValidationResult {
        val errors = when (payload) {
            is QrPayload.PlainText -> required(payload.text, "Enter some text.")
            is QrPayload.Url -> validateUrl(payload.url)
            is QrPayload.Wifi -> buildList {
                if (payload.ssid.isBlank()) add("Enter a Wi-Fi network name (SSID).")
                else if (payload.ssid.toByteArray(StandardCharsets.UTF_8).size > 32) {
                    add("The Wi-Fi network name must be 32 bytes or fewer.")
                }
                if (payload.security != WifiSecurity.NONE && payload.password.isBlank()) {
                    add("Enter the Wi-Fi password, or choose no password.")
                }
            }
            is QrPayload.Geo -> buildList {
                if (!payload.latitude.isFinite() || payload.latitude !in -90.0..90.0) {
                    add("Latitude must be between -90 and 90.")
                }
                if (!payload.longitude.isFinite() || payload.longitude !in -180.0..180.0) {
                    add("Longitude must be between -180 and 180.")
                }
            }
            is QrPayload.VCard -> buildList {
                if (payload.name.isBlank()) add("Enter a contact name.")
                if (payload.phone.isNotBlank() && !isValidPhone(payload.phone)) add("Enter a valid contact phone number.")
                if (payload.email.isNotBlank() && !isValidEmail(payload.email)) add("Enter a valid contact email address.")
                if (payload.url.isNotBlank()) addAll(validateUrl(payload.url))
            }
            is QrPayload.Email -> buildList {
                if (payload.address.isBlank()) add("Enter an email address.")
                else if (!isValidEmail(payload.address)) add("Enter a valid email address.")
            }
            is QrPayload.Telephone -> validatePhone(payload.number)
            is QrPayload.Sms -> validatePhone(payload.number)
            is QrPayload.Unknown -> required(payload.rawValue, "This barcode does not contain any data.")
        }
        return QrValidationResult(errors)
    }

    private fun validateUrl(value: String): List<String> {
        if (value.isBlank()) return listOf("Enter a URL.")
        val normalized = QrPayloadEncoder.normalizeUrl(value)
        return try {
            val uri = URI(normalized)
            if ((uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.rawAuthority.isNullOrBlank()) {
                emptyList()
            } else {
                listOf("Enter a valid HTTP or HTTPS URL.")
            }
        } catch (_: Exception) {
            listOf("Enter a valid HTTP or HTTPS URL.")
        }
    }

    private fun validatePhone(value: String): List<String> = when {
        value.isBlank() -> listOf("Enter a phone number.")
        !isValidPhone(value) -> listOf("Enter a valid phone number.")
        else -> emptyList()
    }

    private fun required(value: String, message: String): List<String> =
        if (value.isBlank()) listOf(message) else emptyList()

    private fun isValidEmail(value: String): Boolean = EMAIL_REGEX.matches(value.trim())

    private fun isValidPhone(value: String): Boolean {
        val trimmed = value.trim()
        val digits = trimmed.count(Char::isDigit)
        return digits in 3..15 && PHONE_REGEX.matches(trimmed)
    }

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$")
    private val PHONE_REGEX = Regex("^\\+?[0-9() .-]+$")
}

object QrActionResolver {
    fun resolve(payload: QrPayload): QrAction {
        if (!QrPayloadValidator.validate(payload).isValid) return QrAction.None
        return when (payload) {
            is QrPayload.Url -> QrAction.OpenUrl(QrPayloadEncoder.normalizeUrl(payload.url))
            is QrPayload.Geo -> QrAction.OpenMap(payload.latitude, payload.longitude)
            is QrPayload.Wifi -> QrAction.ConnectWifi(payload.ssid, payload.password, payload.security)
            is QrPayload.VCard -> QrAction.AddContact(QrPayloadEncoder.encode(payload))
            is QrPayload.Email -> QrAction.SendEmail(payload.address, payload.subject, payload.body)
            is QrPayload.Telephone -> QrAction.Dial(payload.number)
            is QrPayload.Sms -> QrAction.SendSms(payload.number, payload.message)
            is QrPayload.PlainText, is QrPayload.Unknown -> QrAction.None
        }
    }
}
