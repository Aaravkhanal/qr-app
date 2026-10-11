package com.ritikqrscanner.app.scanner

import com.ritikqrscanner.app.ui.screens.shouldShowCameraSwitch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPolicyTest {
    @Test
    fun `switch control only appears when another camera exists`() {
        assertFalse(shouldShowCameraSwitch(0))
        assertFalse(shouldShowCameraSwitch(1))
        assertTrue(shouldShowCameraSwitch(2))
    }
}
