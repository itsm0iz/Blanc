package app.blanc.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.AppInfo
import app.blanc.ui.DrawerMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    apps: List<AppInfo>,
    mode: DrawerMode,
    homeAppsCount: Int,
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

        LazyColumn(modifier = Modifier.fillMaxSize()) {
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

            items(
                items = filtered,
                key = { "${it.packageName}/${it.className}/${it.userSerial}" },
            ) { app ->
                Text(
                    text = app.label,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
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
