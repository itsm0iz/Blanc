package app.blanc.ui.search

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blanc.data.AppInfo
import app.blanc.search.SearchResult
import app.blanc.ui.motion.BlancMotion
import app.blanc.ui.motion.cascadeEnter
import app.blanc.ui.motion.centerEmphasis
import app.blanc.ui.motion.overshootEnter
import app.blanc.ui.motion.rememberHapticTick
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UniversalSearchScreen(
    results: List<SearchResult>,
    motionEnabled: Boolean,
    hapticsEnabled: Boolean,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppInfo) -> Unit,
    onAddAppToHome: (AppInfo) -> Unit,
    onOpenSetting: (String) -> Unit,
    onWebSearch: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val tick = rememberHapticTick(hapticsEnabled)
    var expandedDefinition by remember { mutableStateOf<SearchResult.Definition?>(null) }
    val focusManager = LocalFocusManager.current
    val cascadeSeen = remember { mutableSetOf<Int>() }

    fun submit() {
        val topApp = results.firstOrNull { it is SearchResult.App } as? SearchResult.App
        if (topApp != null) onLaunchApp(topApp.app) else if (query.isNotBlank()) onWebSearch(query)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The search content dims and (on API 31+) blurs behind the definition card.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (expandedDefinition != null && motionEnabled &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ) {
                        Modifier.blur(20.dp)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            // Borderless search: just large bold text with a blinking cursor.
            BasicTextField(
                value = query,
                onValueChange = {
                    query = it
                    onQueryChange(it)
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .overshootEnter(motionEnabled)
                    .focusRequester(focusRequester),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            )
                        }
                        innerTextField()
                    }
                },
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
            ) {
                itemsIndexed(results) { index, result ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .cascadeEnter(index, motionEnabled, cascadeSeen)
                            .centerEmphasis(listState, index, motionEnabled),
                    ) {
                        when (result) {
                            is SearchResult.App -> ResultRow(
                                title = result.app.label,
                                onClick = { onLaunchApp(result.app) },
                                onLongClick = {
                                    tick()
                                    onAddAppToHome(result.app)
                                },
                            )

                            is SearchResult.Calculation -> ResultRow(
                                title = "= ${result.result}",
                                subtitle = result.expression,
                                onClick = { onCopy(result.result) },
                            )

                            is SearchResult.Definition -> DefinitionRow(
                                definition = result,
                                onClick = { expandedDefinition = result },
                            )

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

        expandedDefinition?.let { def ->
            DefinitionCard(
                definition = def,
                motionEnabled = motionEnabled,
                onDismiss = { expandedDefinition = null },
            )
        }
    }

    LaunchedEffect(Unit) {
        onQueryChange("")
        focusRequester.requestFocus()
    }

    // While the card is up, drop the field's focus so the cursor stops blinking
    // (keeping the blurred layer static) and the keyboard retracts; restore both
    // when it closes.
    LaunchedEffect(expandedDefinition) {
        if (expandedDefinition != null) {
            focusManager.clearFocus()
        } else {
            focusRequester.requestFocus()
        }
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
                fontWeight = FontWeight.Bold,
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
private fun DefinitionRow(definition: SearchResult.Definition, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = definition.word,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = definition.text,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
    }
}

/**
 * A tapped definition rises into a translucent, rounded frosted panel centered on
 * screen while the content behind it dims and (on API 31+) blurs. Dismisses on
 * tap-outside or back. When animations are off it appears instantly as a plain,
 * solid panel with no blur.
 */
@Composable
private fun DefinitionCard(
    definition: SearchResult.Definition,
    motionEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val useBlur = motionEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(if (motionEnabled) 0f else 1f) }
    val slidePx = with(LocalDensity.current) { 24.dp.toPx() }

    fun dismiss() {
        if (!motionEnabled) {
            onDismiss()
            return
        }
        scope.launch {
            progress.animateTo(0f, BlancMotion.ContentSpring)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        if (motionEnabled) progress.animateTo(1f, BlancMotion.OvershootSpring)
    }

    // Registered deeper than BlancApp's back handler, so back closes the card first.
    BackHandler(enabled = true) { dismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimming scrim + tap-outside target.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
                .background(Color.Black.copy(alpha = if (useBlur) 0.18f else 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { dismiss() },
        )

        // The frosted panel — rises, fades, and scales in.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = progress.value
                    alpha = p
                    translationY = (1f - p) * slidePx
                    val s = 0.96f + 0.04f * p
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(28.dp))
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = if (useBlur) 0.62f else 0.92f),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* consume taps so they don't fall through to dismiss */ }
                .padding(28.dp),
        ) {
            Text(
                text = definition.word,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = definition.text,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            )
        }
    }
}
