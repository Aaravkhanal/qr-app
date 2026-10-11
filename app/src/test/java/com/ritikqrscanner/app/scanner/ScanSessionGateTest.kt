package com.ritikqrscanner.app.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanSessionGateTest {
    @Test
    fun `only one result is accepted until dismissal`() {
        val gate = ScanSessionGate()

        assertTrue(gate.tryAccept())
        assertFalse(gate.tryAccept())
        gate.dismiss()
        assertTrue(gate.tryAccept())
    }

    @Test
    fun `restored visible result begins locked`() {
        val gate = ScanSessionGate(initiallyLocked = true)

        assertFalse(gate.tryAccept())
        gate.dismiss()
        assertTrue(gate.tryAccept())
    }
}
