package io.legado.app.ui.book.read

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal data class ReadMenuUiState(
    val visible: Boolean = false,
    val containerColor: Color = Color.Black,
    val contentColor: Color = Color.White,
    val title: String = "",
    val chapterName: String? = null,
    val chapterUrl: String? = null,
    val sourceName: String? = null,
    val showTitleAddition: Boolean = false,
    val showBrightnessControl: Boolean = true,
    val brightnessAuto: Boolean = true,
    val brightnessProgress: Int = 120,
    val brightnessOnRight: Boolean = false,
    val seekProgress: Int = 0,
    val seekMax: Int = 0,
    val preEnabled: Boolean = false,
    val nextEnabled: Boolean = false,
    val autoPageEnabled: Boolean = false
)

internal data class ReadMenuSourceAction(
    val label: String,
    val onClick: () -> Unit
)

internal data class ReadMenuOverflowAction(
    val label: String,
    val supportingText: String? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadMenuMaterialContent(
    state: ReadMenuUiState,
    sourceActions: List<ReadMenuSourceAction>,
    overflowActions: List<ReadMenuOverflowAction>,
    overflowVisible: Boolean,
    onDismiss: () -> Unit,
    onOverflowClick: () -> Unit,
    onOverflowDismiss: () -> Unit,
    onTitleClick: () -> Unit,
    onChapterNameClick: () -> Unit,
    onChapterNameLongClick: () -> Unit,
    onSourceActionClick: () -> Unit,
    onBrightnessAutoClick: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onBrightnessChangeFinished: (Float) -> Unit,
    onBrightnessPositionToggle: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekChangeFinished: (Float) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAutoPageClick: () -> Unit,
    onReplaceClick: () -> Unit,
    onNightModeClick: () -> Unit,
    onCatalogClick: () -> Unit,
    onReadAloudClick: () -> Unit,
    onReadAloudLongClick: () -> Unit,
    onLayoutClick: () -> Unit,
    onSettingClick: () -> Unit
) {
    val textColor = state.contentColor
    val topColor = state.containerColor.copy(alpha = 0.94f)
    val bottomColor = state.containerColor.copy(alpha = 0.96f)
    val iconBackground = state.contentColor.copy(alpha = 0.12f)

    Box(modifier = Modifier.fillMaxSize()) {
        if (overflowVisible) {
            ModalBottomSheet(
                onDismissRequest = onOverflowDismiss,
                containerColor = bottomColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "阅读菜单",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    overflowActions.forEach { action ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.Transparent,
                            onClick = {
                                if (action.enabled) {
                                    onOverflowDismiss()
                                    action.onClick()
                                }
                            },
                            enabled = action.enabled
                        ) {
                            Column(modifier = Modifier.padding(vertical = 14.dp)) {
                                Text(
                                    text = action.label,
                                    color = if (action.enabled) textColor else textColor.copy(alpha = 0.38f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                action.supportingText?.let {
                                    Text(
                                        text = it,
                                        color = textColor.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (state.visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )
        }

        AnimatedVisibility(
            visible = state.visible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(28.dp),
                color = topColor
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                tint = textColor
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTitleClick() }
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = state.title,
                                color = textColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.showTitleAddition) {
                                state.chapterName?.let {
                                    Text(
                                        text = it,
                                        color = textColor.copy(alpha = 0.82f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.combinedClickable(
                                            onClick = onChapterNameClick,
                                            onLongClick = onChapterNameLongClick
                                        )
                                    )
                                }
                                state.chapterUrl?.let {
                                    Text(
                                        text = it,
                                        color = textColor.copy(alpha = 0.68f),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.combinedClickable(
                                            onClick = onChapterNameClick,
                                            onLongClick = onChapterNameLongClick
                                        )
                                    )
                                }
                            }
                        }
                        if (!state.sourceName.isNullOrBlank()) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                Surface(
                                    shape = CircleShape,
                                    color = iconBackground,
                                    modifier = Modifier.clickable {
                                        expanded = true
                                        onSourceActionClick()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = state.sourceName,
                                            color = textColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            Icons.Rounded.MoreVert,
                                            contentDescription = null,
                                            tint = textColor,
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .size(16.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    sourceActions.forEach { action ->
                                        DropdownMenuItem(
                                            text = { Text(action.label) },
                                            onClick = {
                                                expanded = false
                                                action.onClick()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(onClick = onOverflowClick) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = null,
                                tint = textColor
                            )
                        }
                    }
                }
            }
        }

        if (state.showBrightnessControl) {
            AnimatedVisibility(
                visible = state.visible,
                modifier = Modifier.align(
                    if (state.brightnessOnRight) Alignment.CenterEnd else Alignment.CenterStart
                ),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = topColor
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = onBrightnessAutoClick) {
                            Icon(
                                imageVector = if (state.brightnessAuto) Icons.Rounded.WbSunny else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = textColor
                            )
                        }
                        var brightnessValue by remember(state.brightnessProgress) {
                            mutableFloatStateOf(state.brightnessProgress.toFloat())
                        }
                        Box(
                            modifier = Modifier
                                .height(180.dp)
                                .width(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Slider(
                                value = brightnessValue,
                                onValueChange = {
                                    brightnessValue = it
                                    onBrightnessChange(it)
                                },
                                onValueChangeFinished = {
                                    onBrightnessChangeFinished(brightnessValue)
                                },
                                valueRange = 1f..255f,
                                enabled = !state.brightnessAuto,
                                modifier = Modifier
                                    .width(160.dp)
                                    .rotate(-90f)
                            )
                        }
                        IconButton(onClick = onBrightnessPositionToggle) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = textColor)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(32.dp),
                color = bottomColor,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MenuCircleAction(Icons.Rounded.Search, "搜索", textColor, iconBackground, onSearchClick)
                        MenuCircleAction(Icons.Rounded.TouchApp, "翻页", textColor, iconBackground, onAutoPageClick)
                        MenuCircleAction(Icons.Rounded.AutoAwesome, "净化", textColor, iconBackground, onReplaceClick)
                        MenuCircleAction(Icons.Rounded.DarkMode, "夜间", textColor, iconBackground, onNightModeClick)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ChapterSeekBar(
                        textColor = textColor,
                        seekProgress = state.seekProgress,
                        seekMax = state.seekMax,
                        preEnabled = state.preEnabled,
                        nextEnabled = state.nextEnabled,
                        onSeekChange = onSeekChange,
                        onSeekChangeFinished = onSeekChangeFinished,
                        onPrevClick = onPrevClick,
                        onNextClick = onNextClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MenuTabAction(Icons.AutoMirrored.Rounded.FormatListBulleted, "目录", textColor, onCatalogClick)
                        MenuTabAction(Icons.Rounded.Headphones, "朗读", textColor, onReadAloudClick, onReadAloudLongClick)
                        MenuTabAction(Icons.Rounded.FormatSize, "界面", textColor, onLayoutClick)
                        MenuTabAction(Icons.Rounded.Settings, "设置", textColor, onSettingClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = background, modifier = Modifier.size(54.dp)) {
            IconButton(onClick = onClick) {
                Icon(icon, contentDescription = null, tint = tint)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun ChapterSeekBar(
    textColor: Color,
    seekProgress: Int,
    seekMax: Int,
    preEnabled: Boolean,
    nextEnabled: Boolean,
    onSeekChange: (Float) -> Unit,
    onSeekChangeFinished: (Float) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
    var sliderValue by remember(seekProgress, seekMax) {
        mutableFloatStateOf(seekProgress.toFloat().coerceIn(0f, seekMax.toFloat().coerceAtLeast(0f)))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "上一章",
            color = if (preEnabled) textColor else textColor.copy(alpha = 0.38f),
            modifier = Modifier.clickable(enabled = preEnabled, onClick = onPrevClick)
        )
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onSeekChange(it)
            },
            onValueChangeFinished = { onSeekChangeFinished(sliderValue) },
            valueRange = 0f..seekMax.toFloat().coerceAtLeast(0f),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )
        Text(
            text = "下一章",
            color = if (nextEnabled) textColor else textColor.copy(alpha = 0.38f),
            modifier = Modifier.clickable(enabled = nextEnabled, onClick = onNextClick)
        )
    }
}

@Composable
private fun MenuTabAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onLongClick != null) {
            Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        } else {
            Modifier.clickable(onClick = onClick)
        }
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint.copy(alpha = 0.76f)
        )
    }
}
