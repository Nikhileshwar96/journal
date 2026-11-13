package com.nikhil.journal.data

import com.nikhil.journal.data.ImageStorage.Companion.calculateInSampleSize
import com.nikhil.journal.data.ImageStorage.Companion.targetSize
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageSizingTest {

    @Test
    fun smallImagesAreLeftAlone() {
        assertEquals(1, calculateInSampleSize(800, 600, 1600))
        assertEquals(800 to 600, targetSize(800, 600, 1600))
        assertEquals(1600 to 900, targetSize(1600, 900, 1600))
    }

    @Test
    fun largeImagesAreSampledButStayAboveTarget() {
        // 4000px long edge -> sample 2 gives 2000 (>=1600), sample 4 would give 1000.
        assertEquals(2, calculateInSampleSize(4000, 3000, 1600))
        assertEquals(8, calculateInSampleSize(3000, 20000, 1600))
    }

    @Test
    fun targetSizeCapsLongEdgeAndKeepsAspectRatio() {
        assertEquals(1600 to 1200, targetSize(2000, 1500, 1600))
        assertEquals(900 to 1600, targetSize(1800, 3200, 1600))
        // Extreme panorama never collapses to zero height.
        assertEquals(1600 to 1, targetSize(100_000, 10, 1600))
    }

    @Test
    fun likePatternEscapesWildcards() {
        assertEquals("%%", JournalDao.likePattern("  "))
        assertEquals("%50\\%\\_off%", JournalDao.likePattern("50%_off"))
        assertEquals("%a\\\\b%", JournalDao.likePattern("a\\b"))
    }
}
