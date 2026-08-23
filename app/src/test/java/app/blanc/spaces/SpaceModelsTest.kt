package app.blanc.spaces

import app.blanc.data.prefs.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SpaceModelsTest {

    private val slot = SpaceSlot(
        AppKey("com.example", "com.example.Main", 4L),
        "Example",
    )

    @Test
    fun emptySpaceHasExactlySixteenStableSlots() {
        val space = BlancSpace.empty("id", "Tools")

        assertEquals(16, space.slots.size)
        assertEquals(16, space.slots.count { it == null })
    }

    @Test
    fun clearingOnePositionDoesNotCompactFollowingPositions() {
        val space = BlancSpace.empty("id", "Tools")
            .withSlot(3, slot)
            .withSlot(4, slot)
            .withSlot(3, null)

        assertNull(space.slots[3])
        assertEquals(slot, space.slots[4])
        assertEquals(16, space.slots.size)
    }

    @Test
    fun invalidPositionIsSafeNoOp() {
        val space = BlancSpace.empty("id", "Tools")

        assertSame(space, space.withSlot(-1, slot))
        assertSame(space, space.withSlot(16, slot))
    }

    @Test
    fun namesAreTrimmedAndBounded() {
        val space = BlancSpace.empty("id", "   ${"x".repeat(40)}   ")

        assertEquals(BlancSpace.MAX_NAME_LENGTH, space.name.length)
    }
}
