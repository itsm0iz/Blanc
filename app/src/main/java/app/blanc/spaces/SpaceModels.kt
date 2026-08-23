package app.blanc.spaces

import app.blanc.data.prefs.AppKey

/** One fixed position inside a [BlancSpace]. The saved label survives uninstalls. */
data class SpaceSlot(
    val appKey: AppKey,
    val savedLabel: String,
)

/** A spatial, always-open folder. Slots never compact when an app disappears. */
data class BlancSpace(
    val id: String,
    val name: String,
    val slots: List<SpaceSlot?>,
) {
    init {
        require(slots.size == SLOT_COUNT) { "A Space must always contain $SLOT_COUNT slots" }
    }

    fun withName(value: String): BlancSpace = copy(name = cleanSpaceName(value))

    fun withSlot(index: Int, slot: SpaceSlot?): BlancSpace {
        if (index !in slots.indices) return this
        return copy(slots = slots.toMutableList().also { it[index] = slot })
    }

    companion object {
        const val SLOT_COUNT = 16
        const val MAX_NAME_LENGTH = 24

        fun empty(id: String, name: String): BlancSpace = BlancSpace(
            id = id,
            name = cleanSpaceName(name),
            slots = List(SLOT_COUNT) { null },
        )
    }
}

/** All configuration for the swipe-left catalogue. */
data class SpacesConfig(
    val enabled: Boolean = true,
    val wallpaperBlur: Boolean = true,
    val spaces: List<BlancSpace> = emptyList(),
) {
    companion object {
        const val MAX_SPACES = 12
        val DEFAULT = SpacesConfig()
    }
}

fun cleanSpaceName(value: String): String =
    value.trim().take(BlancSpace.MAX_NAME_LENGTH).ifBlank { "Untitled" }
