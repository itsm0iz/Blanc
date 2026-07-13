package app.blanc.search

import app.blanc.data.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a query into an ordered list of [SearchResult]s from every provider:
 * a math result, matching apps, a dictionary definition, settings shortcuts,
 * and a web-search fallback. Blank query = all apps (the plain drawer).
 */
class SearchEngine(private val dictionary: Dictionary) {

    private val singleWord = Regex("[A-Za-z]{2,}")

    suspend fun search(query: String, apps: List<AppInfo>): List<SearchResult> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isEmpty()) return@withContext apps.map { SearchResult.App(it) }

            val results = ArrayList<SearchResult>()

            Calculator.evaluate(q)?.let {
                results.add(SearchResult.Calculation(q, it))
            }

            // Explicit "define X" / "def X" surfaces the definition near the top.
            val explicitWord = extractDefineWord(q)
            val explicitDefinition = explicitWord
                ?.let { word -> dictionary.lookup(word)?.let { SearchResult.Definition(word, it) } }
            if (explicitDefinition != null) results.add(explicitDefinition)

            apps.asSequence()
                .filter { it.label.contains(q, ignoreCase = true) }
                .forEach { results.add(SearchResult.App(it)) }

            // Implicit definition: a bare word that isn't an explicit lookup.
            if (explicitDefinition == null && q.matches(singleWord)) {
                dictionary.lookup(q)?.let { results.add(SearchResult.Definition(q, it)) }
            }

            SettingsCatalog.search(q).forEach {
                results.add(SearchResult.SettingShortcut(it.label, it.action))
            }

            results.add(SearchResult.WebSearch(q))
            results
        }

    private fun extractDefineWord(query: String): String? {
        val lower = query.lowercase()
        for (prefix in listOf("define ", "def ", "meaning of ", "dict ")) {
            if (lower.startsWith(prefix)) {
                return query.substring(prefix.length).trim().takeIf { it.isNotEmpty() }
            }
        }
        return null
    }
}
