package app.blanc.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import app.blanc.data.AppInfo
import app.blanc.ui.DrawerMode
import app.blanc.ui.motion.cascadeEnter
import app.blanc.ui.motion.centerEmphasis

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    apps: List<AppInfo>,
    mode: DrawerMode,
    homeAppsCount: Int,
    motionEnabled: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onAssign: (Int, AppInfo) -> Unit,
    onRemove: (Int) -> Unit,
    onAddHome: (AppInfo) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filtered = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    val showRemove = mode is DrawerMode.AssignHome && mode.index < homeAppsCount
    val listState = rememberLazyListState()
    val cascadeSeen = remember { mutableSetOf<Int>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search apps") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (showRemove) {
                item(key = "remove") {
                    Text(
                        text = "✕  Remove from home",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onRemove((mode as DrawerMode.AssignHome).index) },
                            )
                            .padding(vertical = 14.dp),
                    )
                }
            }

            itemsIndexed(
                items = filtered,
                key = { _, app -> "${app.packageName}/${app.className}/${app.userSerial}" },
            ) { index, app ->
                Text(
                    text = app.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .cascadeEnter(index, motionEnabled, cascadeSeen)
                        // Offset by the optional "remove" header so the emphasis
                        // targets this row's true position in the lazy list.
                        .centerEmphasis(listState, index + if (showRemove) 1 else 0, motionEnabled)
                        .combinedClickable(
                            onClick = {
                                when (mode) {
                                    is DrawerMode.Launch -> onLaunch(app)
                                    is DrawerMode.AssignHome -> onAssign(mode.index, app)
                                }
                            },
                            onLongClick = {
                                if (mode is DrawerMode.Launch) onAddHome(app)
                            },
                        )
                        .padding(vertical = 12.dp),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
