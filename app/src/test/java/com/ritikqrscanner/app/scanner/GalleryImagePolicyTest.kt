package com.ritikqrscanner.app.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryImagePolicyTest {
    @Test
    fun `large images are downsampled to a safe working size`() {
        val cases = mapOf(
            (800 to 1_200) to 1,
            (3_200 to 2_400) to 2,
            (8_000 to 6_000) to 8,
            (12_000 to 1_000) to 8
        )

        cases.forEach { (dimensions, expectedSampleSize) ->
            assertEquals(
                "Unexpected sample size for ${dimensions.first}x${dimensions.second}",
                expectedSampleSize,
                calculateGallerySampleSize(dimensions.first, dimensions.second)
            )
        }
    }
}
