package app.blanc.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.AppInfo
import app.blanc.spaces.BlancSpace
import app.blanc.spaces.SpacesConfig
import app.blanc.ui.components.SectionHeader
import app.blanc.ui.components.SettingRow

@Composable
fun SpacesTab(
    config: SpacesConfig,
    apps: List<AppInfo>,
    onToggleEnabled: () -> Unit,
    onToggleBlur: () -> Unit,
    onCreate: (String) -> String,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onSetSlot: (String, Int, AppInfo?) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingOpenId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var pickingSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    val selected = config.spaces.firstOrNull { it.id == selectedId }

    LaunchedEffect(config.spaces, pendingOpenId) {
        val pending = pendingOpenId ?: return@LaunchedEffect
        if (config.spaces.any { it.id == pending }) {
            selectedId = pending
            pendingOpenId = null
        }
    }
    LaunchedEffect(selectedId, config.spaces) {
        if (selectedId != null && selected == null && selectedId != pendingOpenId) {
            selectedId = null
            pickingSlot = null
        }
    }

    BackHandler(enabled = creating || selectedId != null || pickingSlot != null) {
        when {
            pickingSlot != null -> pickingSlot = null
            creating -> creating = false
            else -> selectedId = null
        }
    }

    when {
        selected != null && pickingSlot != null -> SpaceAppPicker(
            space = selected,
            slotIndex = pickingSlot!!,
            apps = apps,
            onBack = { pickingSlot = null },
            onSelect = { app ->
                onSetSlot(selected.id, pickingSlot!!, app)
                pickingSlot = null
            },
            onRemove = {
                onSetSlot(selected.id, pickingSlot!!, null)
                pickingSlot = null
            },
        )

        selected != null -> SpaceEditor(
            space = selected,
            apps = apps,
            position = config.spaces.indexOfFirst { it.id == selected.id },
            totalSpaces = config.spaces.size,
            onBack = { selectedId = null },
            onRename = { onRename(selected.id, it) },
            onPickSlot = { pickingSlot = it },
            onMove = { onMove(selected.id, it) },
            onDelete = {
                onDelete(selected.id)
                selectedId = null
            },
        )

        creating -> CreateSpace(
            onCancel = { creating = false },
            onCreate = { name ->
                pendingOpenId = onCreate(name)
                creating = false
            },
        )

        else -> SpacesOverview(
            config = config,
            onToggleEnabled = onToggleEnabled,
            onToggleBlur = onToggleBlur,
            onSelect = { selectedId = it },
            onCreate = { creating = true },
        )
    }
}

@Composable
private fun SpacesOverview(
    config: SpacesConfig,
    onToggleEnabled: () -> Unit,
    onToggleBlur: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        SectionHeader("Spaces")
        SettingRow("Swipe-left Spaces", if (config.enabled) "On" else "Off", onToggleEnabled)
        SettingRow(
            label = "Wallpaper glass",
            value = if (config.wallpaperBlur) "Blur when available" else "Translucent only",
            onClick = onToggleBlur,
        )
        Text(
            text = "Blur is used only while Spaces is open. Unsupported devices and battery saver automatically use the translucent fallback.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )

        SectionHeader("Your Spaces")
        config.spaces.forEach { space ->
            SettingRow(
                label = space.name,
                value = "${space.slots.count { it != null }}/16",
                onClick = { onSelect(space.id) },
            )
        }
        if (config.spaces.size < SpacesConfig.MAX_SPACES) {
            SettingRow("+ New Space", onClick = onCreate)
        }
        if (config.spaces.isEmpty()) {
            Text(
                text = "Create a Space, then tap its exact 4×4 positions to place apps.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun CreateSpace(onCancel: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        BackRow("New Space", onCancel)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(BlancSpace.MAX_NAME_LENGTH) },
            label = { Text("Name") },
            placeholder = { Text("Work, Social, Tools…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        )
        SettingRow(
            label = "Create Space",
            value = if (name.isBlank()) "Name required" else "Continue",
            onClick = { if (name.isNotBlank()) onCreate(name) },
        )
        SettingRow("Cancel", onClick = onCancel)
    }
}

@Composable
private fun SpaceEditor(
    space: BlancSpace,
    apps: List<AppInfo>,
    position: Int,
    totalSpaces: Int,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onPickSlot: (Int) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable(space.id) { mutableStateOf(space.name) }
    var confirmDelete by rememberSaveable(space.id) { mutableStateOf(false) }
    val appsByKey = remember(apps) { apps.associateBy { it.key } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        BackRow(space.name, onBack)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(BlancSpace.MAX_NAME_LENGTH) },
            label = { Text("Space name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, start = 4.dp, end = 4.dp),
        )
        if (name.trim() != space.name && name.isNotBlank()) {
            SettingRow("Save name", "Apply", onClick = { onRename(name) })
        }

        SectionHeader("4 × 4 positions")
        Text(
            text = "Tap a position to assign, replace, or remove its app. Empty Spaces stay hidden from the launcher.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            modifier = Modifier.padding(bottom = 10.dp),
        )
        EditorGrid(space, appsByKey, onPickSlot)

        SectionHeader("Order")
        if (position > 0) SettingRow("Move earlier", "↑", onClick = { onMove(-1) })
        if (position < totalSpaces - 1) SettingRow("Move later", "↓", onClick = { onMove(1) })

        SectionHeader("Remove")
        if (confirmDelete) {
            Text(
                text = "Delete “${space.name}”? Its app layout cannot be restored.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                modifier = Modifier.padding(vertical = 8.dp),
            )
            SettingRow("Confirm delete", onClick = onDelete)
            SettingRow("Keep Space", onClick = { confirmDelete = false })
        } else {
            SettingRow("Delete Space", onClick = { confirmDelete = true })
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun EditorGrid(
    space: BlancSpace,
    appsByKey: Map<app.blanc.data.prefs.AppKey, AppInfo>,
    onPickSlot: (Int) -> Unit,
) {
    val cellColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.055f)
    repeat(4) { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(4) { column ->
                val index = row * 4 + column
                val slot = space.slots[index]
                val app = slot?.let { appsByKey[it.appKey] }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cellColor)
                        .clickable { onPickSlot(index) }
                        .padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = app?.label ?: slot?.savedLabel ?: "+",
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        fontSize = if (slot == null) 20.sp else 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = if (slot == null) FontWeight.Normal else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = when {
                                slot == null -> 0.32f
                                app == null -> 0.35f
                                else -> 0.9f
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpaceAppPicker(
    space: BlancSpace,
    slotIndex: Int,
    apps: List<AppInfo>,
    onBack: () -> Unit,
    onSelect: (AppInfo) -> Unit,
    onRemove: () -> Unit,
) {
    var query by rememberSaveable(space.id, slotIndex) { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val value = query.trim()
        if (value.isEmpty()) apps else apps.filter {
            it.label.contains(value, ignoreCase = true) ||
                it.packageName.contains(value, ignoreCase = true)
        }
    }
    val duplicateLabels = remember(apps) {
        apps.groupingBy { it.label.lowercase() }.eachCount().filterValues { it > 1 }.keys
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        BackRow("Position ${slotIndex + 1}", onBack)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (space.slots[slotIndex] != null) {
                item(key = "remove") {
                    Text(
                        text = "Remove from this position",
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRemove)
                            .padding(vertical = 15.dp),
                    )
                }
            }
            items(
                items = filtered,
                key = { "${it.packageName}/${it.className}/${it.userSerial}" },
            ) { app ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(app) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(
                        text = app.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (app.label.lowercase() in duplicateLabels) {
                        Text(
                            text = "${app.packageName} · profile ${app.userSerial}",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.42f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackRow(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "‹",
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
