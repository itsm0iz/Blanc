package app.blanc.ui.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.AppInfo
import app.blanc.search.SearchResult
import app.blanc.ui.motion.cascadeEnter
import app.blanc.ui.motion.overshootEnter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UniversalSearchScreen(
    results: List<SearchResult>,
    motionEnabled: Boolean,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onAddAppToHome: (AppInfo) -> Unit,
    onOpenSetting: (String) -> Unit,
    onWebSearch: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun submit() {
        val topApp = results.firstOrNull { it is SearchResult.App } as? SearchResult.App
        if (topApp != null) onLaunchApp(topApp.app) else if (query.isNotBlank()) onWebSearch(query)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onQueryChange(it)
            },
            singleLine = true,
            placeholder = { Text("Search apps, math, words, settings…") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .overshootEnter(motionEnabled)
                .focusRequester(focusRequester),
        )

        val cascade = motionEnabled && query.isEmpty()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(results) { index, result ->
                Box(Modifier.fillMaxWidth().cascadeEnter(index, cascade)) {
                    when (result) {
                        is SearchResult.App -> ResultRow(
                            title = result.app.label,
                            onClick = { onLaunchApp(result.app) },
                            onLongClick = { onAddAppToHome(result.app) },
                        )

                        is SearchResult.Calculation -> ResultRow(
                            title = "= ${result.result}",
                            subtitle = result.expression,
                            onClick = { onCopy(result.result) },
                        )

                        is SearchResult.Definition -> DefinitionRow(result)

                        is SearchResult.SettingShortcut -> ResultRow(
                            title = result.label,
                            subtitle = "Settings",
                            onClick = { onOpenSetting(result.action) },
                        )

                        is SearchResult.WebSearch -> ResultRow(
                            title = "Search the web for “${result.query}”",
                            onClick = { onWebSearch(result.query) },
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        onQueryChange("")
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun DefinitionRow(definition: SearchResult.Definition) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = definition.word,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = definition.text,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
    }
}
