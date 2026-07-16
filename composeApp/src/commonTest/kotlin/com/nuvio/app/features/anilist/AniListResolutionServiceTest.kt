package com.nuvio.app.features.anilist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AniListResolutionServiceTest {

    @Test
    fun testParseDateString() {
        // Epoch start
        val epochMs = AniListResolutionService.parseDateString("1970-01-01")
        assertEquals(0L, epochMs)
        
        // Date with ISO time signature vs clean date
        val dateWithTime = AniListResolutionService.parseDateString("2023-11-04T12:00:00.000Z")
        val dateClean = AniListResolutionService.parseDateString("2023-11-04")
        assertEquals(dateClean, dateWithTime)
    }

    @Test
    fun testAreDatesClose() {
        // Under tolerance limit
        assertTrue(AniListResolutionService.areDatesClose("2023-03-03", "2023-03-05", 2))
        assertTrue(AniListResolutionService.areDatesClose("2023-03-03", "2023-03-01", 2))
        
        // Exact same day
        assertTrue(AniListResolutionService.areDatesClose("2023-03-03", "2023-03-03", 2))
        
        // Exceeds tolerance limit
        assertFalse(AniListResolutionService.areDatesClose("2023-03-03", "2023-03-06", 2))
        assertFalse(AniListResolutionService.areDatesClose("2023-03-03", "2023-02-28", 2))
    }
}
