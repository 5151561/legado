package io.legado.app.ui.compose.theme

import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/**
 * 封装原生 Material 3 Switch
 * 1. 统一缩小到 75% 尺寸
 * 2. 内部维护 localChecked 确保动画即时反馈，不受数据库延迟影响
 */
@Composable
fun LegadoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // 使用 localChecked 记录当前 UI 状态，确保开关动画能立即执行
    var localChecked by remember(checked) { mutableStateOf(checked) }

    // 当外部状态（如数据库加载完成）改变时，同步 localChecked
    LaunchedEffect(checked) {
        localChecked = checked
    }

    Switch(
        checked = localChecked,
        onCheckedChange = { 
            localChecked = it 
            onCheckedChange?.invoke(it)
        },
        modifier = modifier.scale(0.75f),
        enabled = enabled
    )
}
