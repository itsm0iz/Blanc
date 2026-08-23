package app.blanc.spaces

import app.blanc.data.prefs.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacesCodecTest {

    private val browser = SpaceSlot(
        appKey = AppKey("com.example.browser", "com.example.browser.Main", 0L),
        savedLabel = "Browser",
    )

    @Test
    fun roundTripPreservesUnicodeOrderAndFixedPositions() {
        val first = BlancSpace.empty("first", "Café 日本語")
            .withSlot(0, browser)
            .withSlot(15, SpaceSlot(AppKey("app.notes", "app.notes.Home", 12L), "Notes & ideas"))
        val second = BlancSpace.empty("second", "Work\tStudy")
            .withSlot(7, browser)
        val original = SpacesConfig(
            enabled = false,
            wallpaperBlur = false,
            spaces = listOf(first, second),
        )

        val decoded = SpacesCodec.decode(SpacesCodec.encode(original))

        assertEquals(original, decoded)
        assertEquals(BlancSpace.SLOT_COUNT, decoded.spaces.first().slots.size)
        assertNull(decoded.spaces.first().slots[1])
        assertEquals(browser, decoded.spaces.first().slots[0])
        assertEquals(browser, decoded.spaces[1].slots[7])
    }

    @Test
    fun malformedSlotDoesNotShiftOrDestroyOtherSlots() {
        val original = SpacesConfig(spaces = listOf(
            BlancSpace.empty("one", "Tools")
                .withSlot(0, browser)
                .withSlot(2, browser),
        ))
        val lines = SpacesCodec.encode(original).lines().toMutableList()
        val row = lines[1].split('\t').toMutableList()
        row[3] = "%ZZ:broken"
        lines[1] = row.joinToString("\t")

        val decoded = SpacesCodec.decode(lines.joinToString("\n"))

        assertEquals(1, decoded.spaces.size)
        assertEquals(browser, decoded.spaces.single().slots[0])
        assertNull(decoded.spaces.single().slots[1])
        assertEquals(browser, decoded.spaces.single().slots[2])
        assertEquals(BlancSpace.SLOT_COUNT, decoded.spaces.single().slots.size)
    }

    @Test
    fun invalidHeaderFallsBackSafely() {
        val decoded = SpacesCodec.decode("blanc-spaces\t99\t0\t0")

        assertEquals(SpacesConfig.DEFAULT, decoded)
        assertTrue(decoded.enabled)
        assertTrue(decoded.wallpaperBlur)
    }

    @Test
    fun flagsRoundTripIndependently() {
        val decoded = SpacesCodec.decode(SpacesCodec.encode(
            SpacesConfig(enabled = true, wallpaperBlur = false),
        ))

        assertTrue(decoded.enabled)
        assertFalse(decoded.wallpaperBlur)
    }
}
