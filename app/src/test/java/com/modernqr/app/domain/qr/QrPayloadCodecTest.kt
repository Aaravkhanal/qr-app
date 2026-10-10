package com.modernqr.app.domain.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPayloadCodecTest {
    @Test
    fun `generated payloads round trip with the same semantic values`() {
        val payloads = listOf(
            QrPayload.PlainText("A normal multiline value\nwith Unicode: नमस्ते 🚀"),
            QrPayload.Url("https://example.com/a?name=Aarav#section"),
            QrPayload.Wifi(
                ssid = "Office;Guest:5G,East\\Wing\nFloor 2",
                password = "p;ass:word,\\line\nnext",
                security = WifiSecurity.WPA,
                hidden = true
            ),
            QrPayload.Wifi("Open network", security = WifiSecurity.NONE),
            QrPayload.Geo(27.7172, 85.324),
            QrPayload.VCard(
                name = "Khanal, Aarav; देव",
                phone = "+977 981-234-5678",
                email = "aarav@example.com",
                url = "https://example.com/profile:a",
                note = "Line one\nLine two; colon: comma, slash\\"
            ),
            QrPayload.Email(
                address = "aarav+qr@example.com",
                subject = "Status: ready; yes",
                body = "First line\nSecond: line, with \\ slash; done"
            ),
            QrPayload.Telephone("+1 (212) 555-0100"),
            QrPayload.Sms("+9779812345678", "Reminder: arrive at 10:30; bring ID, please\\thanks")
        )

        payloads.forEach { payload ->
            val encoded = QrPayloadEncoder.encode(payload)
            assertEquals("Failed round trip for $encoded", payload, QrPayloadParser.parse(encoded))
        }
    }

    @Test
    fun `parser handles representative external payloads case insensitively`() {
        data class Case(val encoded: String, val expected: QrPayload)

        val cases = listOf(
            Case("HTTPS://Example.com/Path", QrPayload.Url("HTTPS://Example.com/Path")),
            Case("wifi:t:WEP;s:Cafe\\; East;p:a\\:b;;", QrPayload.Wifi("Cafe; East", "a:b", WifiSecurity.WEP)),
            Case("GEO:-33.86,151.21", QrPayload.Geo(-33.86, 151.21)),
            Case(
                "begin:vcard\nversion:3.0\nfn:Zoë\ntel:+33123456789\nend:vcard",
                QrPayload.VCard("Zoë", "+33123456789")
            ),
            Case(
                "matmsg:to:user@example.com;sub:Hi\\: there;body:A\\;B;;",
                QrPayload.Email("user@example.com", "Hi: there", "A;B")
            ),
            Case(
                "MAILTO:user+tag@example.com?subject=Hello+there&body=Line%201%0ALine%202",
                QrPayload.Email("user+tag@example.com", "Hello there", "Line 1\nLine 2")
            ),
            Case("TEL:+442071838750", QrPayload.Telephone("+442071838750")),
            Case("SMSTO:+12025550123:Meet: lobby: 10:30", QrPayload.Sms("+12025550123", "Meet: lobby: 10:30")),
            Case("example.org/docs", QrPayload.Url("https://example.org/docs")),
            Case("person@example.org", QrPayload.Email("person@example.org"))
        )

        cases.forEach { case ->
            assertEquals("Failed to parse ${case.encoded}", case.expected, QrPayloadParser.parse(case.encoded))
        }
    }

    @Test
    fun `normalizing a URL preserves existing custom schemes`() {
        val cases = mapOf(
            "example.com" to "https://example.com",
            "HTTP://example.com" to "HTTP://example.com",
            "myapp://screen/item:42" to "myapp://screen/item:42",
            "custom:value" to "custom:value"
        )

        cases.forEach { (input, expected) ->
            assertEquals(expected, QrPayloadEncoder.normalizeUrl(input))
        }
    }

    @Test
    fun `unknown non QR barcode remains meaningful`() {
        assertEquals(
            QrPayload.Unknown("0123456789012", "EAN_13"),
            QrPayloadParser.parse("0123456789012", "EAN_13")
        )
        assertEquals(QrPayload.PlainText("0123456789012"), QrPayloadParser.parse("0123456789012"))
    }

    @Test
    fun `malformed recognized payloads retain their type for validation`() {
        val malformed = listOf(
            QrPayloadParser.parse("WIFI:T:WPA;S:;P:;;"),
            QrPayloadParser.parse("MATMSG:TO:not-an-email;SUB:;BODY:;;"),
            QrPayloadParser.parse("tel:not-a-number"),
            QrPayloadParser.parse("smsto::hello")
        )

        malformed.forEach { assertFalse(QrPayloadValidator.validate(it).isValid) }
    }
}

class QrPayloadValidatorTest {
    @Test
    fun `validation table returns friendly errors`() {
        data class Case(val payload: QrPayload, val expectedMessage: String)

        val invalidCases = listOf(
            Case(QrPayload.PlainText("\n  "), "Enter some text."),
            Case(QrPayload.Url(""), "Enter a URL."),
            Case(QrPayload.Url("ftp://example.com"), "Enter a valid HTTP or HTTPS URL."),
            Case(QrPayload.Url("https://bad host"), "Enter a valid HTTP or HTTPS URL."),
            Case(QrPayload.Wifi("", "password"), "Enter a Wi-Fi network name (SSID)."),
            Case(QrPayload.Wifi("📶".repeat(9), security = WifiSecurity.NONE), "The Wi-Fi network name must be 32 bytes or fewer."),
            Case(QrPayload.Wifi("Office", "", WifiSecurity.WPA), "Enter the Wi-Fi password, or choose no password."),
            Case(QrPayload.Geo(90.1, 0.0), "Latitude must be between -90 and 90."),
            Case(QrPayload.Geo(0.0, -180.1), "Longitude must be between -180 and 180."),
            Case(QrPayload.VCard(""), "Enter a contact name."),
            Case(QrPayload.VCard("Name", phone = "call-me"), "Enter a valid contact phone number."),
            Case(QrPayload.VCard("Name", email = "invalid@"), "Enter a valid contact email address."),
            Case(QrPayload.Email("missing-at.example.com"), "Enter a valid email address."),
            Case(QrPayload.Telephone("12"), "Enter a valid phone number."),
            Case(QrPayload.Sms("", "hello"), "Enter a phone number."),
            Case(QrPayload.Unknown("", "EAN_13"), "This barcode does not contain any data.")
        )

        invalidCases.forEach { case ->
            val result = QrPayloadValidator.validate(case.payload)
            assertFalse("Expected ${case.payload} to be invalid", result.isValid)
            assertEquals(case.expectedMessage, result.message)
        }
    }

    @Test
    fun `normal Unicode and boundary values validate`() {
        val validCases = listOf(
            QrPayload.PlainText("नमस्ते\n世界"),
            QrPayload.Url("example.com"),
            QrPayload.Wifi("क्याफे 📶", security = WifiSecurity.NONE),
            QrPayload.Geo(-90.0, 180.0),
            QrPayload.VCard("Zoë 李", phone = "+33 1 23 45 67 89", email = "zoe@example.fr"),
            QrPayload.Email("user+tag@example.co.uk"),
            QrPayload.Telephone("911"),
            QrPayload.Sms("+977-9812345678", "नमस्ते")
        )

        validCases.forEach { payload ->
            assertTrue("Expected $payload to be valid", QrPayloadValidator.validate(payload).isValid)
        }
    }
}

class QrActionResolverTest {
    @Test
    fun `action table resolves supported payloads`() {
        val cases = mapOf<QrPayload, QrAction>(
            QrPayload.Url("example.com") to QrAction.OpenUrl("https://example.com"),
            QrPayload.Geo(1.0, 2.0) to QrAction.OpenMap(1.0, 2.0),
            QrPayload.Wifi("Cafe", "secret") to QrAction.ConnectWifi("Cafe", "secret", WifiSecurity.WPA),
            QrPayload.Email("a@example.com", "Hi", "Body") to QrAction.SendEmail("a@example.com", "Hi", "Body"),
            QrPayload.Telephone("12345") to QrAction.Dial("12345"),
            QrPayload.Sms("12345", "a:b") to QrAction.SendSms("12345", "a:b"),
            QrPayload.PlainText("hello") to QrAction.None,
            QrPayload.Unknown("123", "CODE_128") to QrAction.None
        )

        cases.forEach { (payload, expected) ->
            assertEquals(expected, QrActionResolver.resolve(payload))
        }
    }

    @Test
    fun `vCard action contains a parseable contact`() {
        val payload = QrPayload.VCard("Aarav", "+9779812345678", "aarav@example.com")
        val action = QrActionResolver.resolve(payload) as QrAction.AddContact
        assertEquals(payload, QrPayloadParser.parse(action.encodedVCard))
    }
}
