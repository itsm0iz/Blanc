package app.blanc.spaces

import app.blanc.data.prefs.AppKey
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Compact, versioned and Unicode-safe persistence for Spaces.
 *
 * One preference value keeps the entire catalogue atomic. Tabs/newlines and
 * punctuation are URL-encoded, while malformed rows or slots degrade locally
 * instead of destroying the rest of the user's layout.
 */
object SpacesCodec {
    private const val MAGIC = "blanc-spaces"
    private const val VERSION = "1"
    private const val EMPTY_SLOT = "-"

    fun encode(config: SpacesConfig): String = buildString {
        append(MAGIC)
        append('\t')
        append(VERSION)
        append('\t')
        append(if (config.enabled) '1' else '0')
        append('\t')
        append(if (config.wallpaperBlur) '1' else '0')

        config.spaces.take(SpacesConfig.MAX_SPACES).forEach { space ->
            append('\n')
            append(escape(space.id))
            append('\t')
            append(escape(space.name))
            space.slots.forEach { slot ->
                append('\t')
                if (slot == null) {
                    append(EMPTY_SLOT)
                } else {
                    append(escape(slot.appKey.encode()))
                    append(':')
                    append(escape(slot.savedLabel))
                }
            }
        }
    }

    fun decode(value: String?): SpacesConfig {
        if (value.isNullOrBlank()) return SpacesConfig.DEFAULT
        val lines = value.lineSequence().toList()
        val header = lines.firstOrNull()?.split('\t') ?: return SpacesConfig.DEFAULT
        if (header.size != 4 || header[0] != MAGIC || header[1] != VERSION) {
            return SpacesConfig.DEFAULT
        }

        val spaces = lines.drop(1)
            .mapNotNull(::decodeSpace)
            .distinctBy { it.id }
            .take(SpacesConfig.MAX_SPACES)

        return SpacesConfig(
            enabled = header[2] != "0",
            wallpaperBlur = header[3] != "0",
            spaces = spaces,
        )
    }

    private fun decodeSpace(line: String): BlancSpace? {
        val fields = line.split('\t')
        if (fields.size != 2 + BlancSpace.SLOT_COUNT) return null
        val id = unescape(fields[0])?.takeIf { it.isNotBlank() } ?: return null
        val name = unescape(fields[1]) ?: return null
        val slots = fields.drop(2).map(::decodeSlot)
        return BlancSpace(id = id, name = cleanSpaceName(name), slots = slots)
    }

    private fun decodeSlot(value: String): SpaceSlot? {
        if (value == EMPTY_SLOT) return null
        val parts = value.split(':', limit = 2)
        if (parts.size != 2) return null
        val key = unescape(parts[0])?.let { AppKey.decode(it) } ?: return null
        val label = unescape(parts[1])
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: key.packageName.substringAfterLast('.')
        return SpaceSlot(key, label)
    }

    private fun escape(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun unescape(value: String): String? = runCatching {
        URLDecoder.decode(value, Charsets.UTF_8.name())
    }.getOrNull()
}
