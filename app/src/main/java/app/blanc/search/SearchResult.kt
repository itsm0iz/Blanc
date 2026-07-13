package app.blanc.search

import app.blanc.data.AppInfo

/** A single row in the universal search results, tagged by its kind. */
sealed interface SearchResult {
    data class App(val app: AppInfo) : SearchResult
    data class Calculation(val expression: String, val result: String) : SearchResult
    data class Definition(val word: String, val text: String) : SearchResult
    data class SettingShortcut(val label: String, val action: String) : SearchResult
    data class WebSearch(val query: String) : SearchResult
}
