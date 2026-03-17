import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// --- 核心品牌色系定义 ---
val BrandBlue = Color(0xFF4A5B7C)
val IslandBg = Color(0xFFE4E9ED)
val ControlBg = Color(0x66CBD5E1) // 类似 slate-300/40
val TextDark = Color(0xFF334155) // slate-700
val TextMuted = Color(0xFF64748B) // slate-500

@Preview(showBackground = true, device = "id:pixel_7_pro")
@Composable
fun ReaderSettingsScreenPreview() {
    MaterialTheme {
        ReaderSettingsScreen()
    }
}

@Composable
fun ReaderSettingsScreen() {
    // === 状态管理 (对应 React 的 useState) ===
    var activeTab by remember { mutableStateOf("font") }
    var isTraditional by remember { mutableStateOf(false) }
    var activeTheme by remember { mutableStateOf("预设1") }

    // 字体排版状态
    var fontSize by remember { mutableFloatStateOf(20f) }
    var letterSpacing by remember { mutableFloatStateOf(0.3f) }
    var lineHeight by remember { mutableFloatStateOf(1.5f) }
    var paragraphSpacing by remember { mutableFloatStateOf(0.2f) }
    var textWeight by remember { mutableFloatStateOf(400f) }
    var pageTurn by remember { mutableStateOf("覆盖") }
    var sharedLayout by remember { mutableStateOf(false) }

    // 边距状态 (简化为一个统一样例，实际开发可用 Data Class)
    var headerShowLine by remember { mutableStateOf(true) }
    var headerTop by remember { mutableFloatStateOf(10f) }
    var headerBottom by remember { mutableFloatStateOf(0f) }
    var headerLeft by remember { mutableFloatStateOf(19f) }
    var headerRight by remember { mutableFloatStateOf(16f) }

    var bodyTop by remember { mutableFloatStateOf(5f) }
    var bodyBottom by remember { mutableFloatStateOf(4f) }
    var bodyLeft by remember { mutableFloatStateOf(22f) }
    var bodyRight by remember { mutableFloatStateOf(22f) }

    // 背景颜色跟随主题变化
    val screenBgColor by animateColorAsState(
        targetValue = if (activeTheme == "夜间") Color(0xFF1A1A1A) else Color(0xFFF0F4F8),
        label = "bg_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
    ) {
        // 1. 模拟底层阅读器文本
        Text(
            text = if (isTraditional) {
                "第1章 (1)\n\n地下室的面積不大，下來之後，在手電的照射下，我一眼就看到地下室裡有一個非常大的鐵籠子..."
            } else {
                "第1章 (1)\n\n地下室的面积不大，下来之后，在手电的照射下，我一眼就看到地下室里有一个非常大的铁笼子..."
            },
            color = if (activeTheme == "夜间") Color.Gray else Color.DarkGray,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * lineHeight).sp,
            fontWeight = FontWeight(textWeight.roundToInt()),
            modifier = Modifier
                .padding(24.dp)
                .padding(top = 40.dp)
                .alpha(0.4f)
        )

        // 2. MD3 浮岛主容器 (对齐到底部)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(IslandBg) // 浅灰蓝浮岛背景
                .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
        ) {

            // --- 浮岛内部可滚动内容区 ---
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.6f) // 控制最大高度，类似 max-h-[60vh]
                    .verticalScroll(rememberScrollState())
            ) {
                when (activeTab) {
                    "font" -> FontParagraphView(
                        isTraditional, { isTraditional = it },
                        textWeight, { textWeight = it },
                        fontSize, { fontSize = it },
                        letterSpacing, { letterSpacing = it },
                        lineHeight, { lineHeight = it },
                        paragraphSpacing, { paragraphSpacing = it },
                        pageTurn, { pageTurn = it },
                        sharedLayout, { sharedLayout = it },
                        activeTheme, { activeTheme = it }
                    )
                    "margins" -> MarginsView(
                        headerShowLine, { headerShowLine = it },
                        headerTop, { headerTop = it },
                        headerBottom, { headerBottom = it },
                        headerLeft, { headerLeft = it },
                        headerRight, { headerRight = it },
                        bodyTop, { bodyTop = it },
                        bodyBottom, { bodyBottom = it },
                        bodyLeft, { bodyLeft = it },
                        bodyRight, { bodyRight = it }
                    )
                    "info" -> InfoSettingsView()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // --- 底部统一导航栏 (返回 + Tabs) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧大返回按钮
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    modifier = Modifier.size(52.dp),
                    shadowElevation = 2.dp,
                    onClick = { /* TODO: 返回正文逻辑 */ }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "返回",
                        modifier = Modifier.padding(14.dp),
                        tint = TextDark
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧分段式底栏
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ControlBg)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        Triple("font", "字体", Icons.Rounded.TextFields),
                        Triple("margins", "边距", Icons.AutoMirrored.Rounded.ViewQuilt),
                        Triple("info", "信息", Icons.AutoMirrored.Rounded.FormatAlignLeft)
                    )

                    tabs.forEach { (id, label, icon) ->
                        val isSelected = activeTab == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null // 移除默认水波纹以模拟现代平滑切换
                                ) { activeTab = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) BrandBlue else TextMuted,
                                    modifier = Modifier.size(20.dp).scale(if (isSelected) 1f else 0.95f)
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BrandBlue else TextMuted,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ================= 各个子视图组件 =================

@Composable
fun FontParagraphView(
    isTraditional: Boolean, onTradChange: (Boolean) -> Unit,
    textWeight: Float, onWeightChange: (Float) -> Unit,
    fontSize: Float, onSizeChange: (Float) -> Unit,
    letterSpacing: Float, onLetterChange: (Float) -> Unit,
    lineHeight: Float, onLineChange: (Float) -> Unit,
    paragraphSpacing: Float, onParaChange: (Float) -> Unit,
    pageTurn: String, onTurnChange: (String) -> Unit,
    sharedLayout: Boolean, onSharedChange: (Boolean) -> Unit,
    activeTheme: String, onThemeChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // 1. 字体
        SectionGroup("字体") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                SegmentedControl(
                    options = listOf("简体", "繁体"),
                    selectedOption = if (isTraditional) "繁体" else "简体",
                    onOptionSelected = { onTradChange(it == "繁体") }
                )
            }
            ListItemArrow("中文字体", "系统默认")
            ListItemArrow("英文字体", "系统默认")
            SettingSlider("粗细", textWeight, 100f, 900f, 100f, onWeightChange, if(textWeight == 400f) "正常" else textWeight.roundToInt().toString())
        }

        // 2. 排版
        SectionGroup("排版") {
            SettingSlider("字号", fontSize, 12f, 36f, 1f, onSizeChange)
            SettingSlider("字距", letterSpacing, 0f, 1f, 0.1f, onLetterChange)
            SettingSlider("行距", lineHeight, 1f, 2.5f, 0.1f, onLineChange)
            SettingSlider("段距", paragraphSpacing, 0f, 1f, 0.1f, onParaChange)
        }

        // 3. 翻页动画
        SectionGroup("翻页动画") {
            SegmentedControl(
                options = listOf("覆盖", "滑动", "仿真", "滚动", "无"),
                selectedOption = pageTurn,
                onOptionSelected = onTurnChange,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. 色彩主题
        SectionGroup(title = "色彩主题", action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("共用布局", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = sharedLayout,
                    onCheckedChange = onSharedChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandBlue)
                )
            }
        }) {
            val themes = listOf("预设1" to Color.White, "预设2" to Color(0xFFF4E8D3), "预设3" to Color(0xFFCCE8CF), "预设4" to Color(0xFFE8D1EA), "预设5" to Color(0xFFC6D7E8), "夜间" to Color(0xFF2C2C2C))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                themes.forEach { (name, color) ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (activeTheme == name) 2.dp else 1.dp,
                                color = if (activeTheme == name) BrandBlue else Color.Black.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .clickable { onThemeChange(name) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name, fontSize = 12.sp, color = if (name == "夜间") Color.White else TextDark)
                    }
                }
            }
        }
    }
}

@Composable
fun MarginsView(
    headerShowLine: Boolean, onHeaderShowLine: (Boolean) -> Unit,
    headerTop: Float, onHTop: (Float) -> Unit, headerBottom: Float, onHBot: (Float) -> Unit, headerLeft: Float, onHLeft: (Float) -> Unit, headerRight: Float, onHRight: (Float) -> Unit,
    bodyTop: Float, onBTop: (Float) -> Unit, bodyBottom: Float, onBBot: (Float) -> Unit, bodyLeft: Float, onBLeft: (Float) -> Unit, bodyRight: Float, onBRight: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SectionGroup(title = "页眉", action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("显示分隔线", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(end = 8.dp))
                Switch(checked = headerShowLine, onCheckedChange = onHeaderShowLine, colors = SwitchDefaults.colors(checkedTrackColor = BrandBlue))
            }
        }) {
            SettingSlider("上边距", headerTop, 0f, 50f, 1f, onHTop)
            SettingSlider("下边距", headerBottom, 0f, 50f, 1f, onHBot)
            SettingSlider("左边距", headerLeft, 0f, 50f, 1f, onHLeft)
            SettingSlider("右边距", headerRight, 0f, 50f, 1f, onHRight)
        }

        SectionGroup("正文") {
            SettingSlider("上边距", bodyTop, 0f, 50f, 1f, onBTop)
            SettingSlider("下边距", bodyBottom, 0f, 50f, 1f, onBBot)
            SettingSlider("左边距", bodyLeft, 0f, 50f, 1f, onBLeft)
            SettingSlider("右边距", bodyRight, 0f, 50f, 1f, onBRight)
        }
    }
}

@Composable
fun InfoSettingsView() {
    // 省略部分状态，使用静态展示代替，实际开发按需补充 State
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SectionGroup("正文标题") {
            SegmentedControl(listOf("靠左", "居中", "隐藏"), "靠左", {}, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            SettingSlider("字号", 4f, 0f, 20f, 1f, {})
            SettingSlider("上边距", 0f, 0f, 50f, 1f, {})
            SettingSlider("下边距", 0f, 0f, 50f, 1f, {})
        }
        SectionGroup("页眉") {
            ListItemArrow("显示/隐藏", "状态栏显示时隐藏")
            ListItemArrow("左侧内容", "标题")
            ListItemArrow("中间内容", "无")
            ListItemArrow("右侧内容", "时间")
        }
    }
}


// ================= 基础原子组件 =================

@Composable
fun SectionGroup(title: String, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
            action?.invoke()
        }
        content()
    }
}

@Composable
fun SettingSlider(
    label: String, value: Float, min: Float, max: Float, step: Float,
    onValueChange: (Float) -> Unit, displayValue: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 14.sp, color = TextDark, modifier = Modifier.width(48.dp))

        // 减号按钮
        IconButton(
            onClick = { onValueChange(max(min, value - step)) },
            modifier = Modifier.size(32.dp).clip(CircleShape).background(ControlBg)
        ) { Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = TextMuted, modifier = Modifier.size(18.dp)) }

        // 滑块轨道
        Slider(
            value = value,
            onValueChange = {
                // 确保步进值 (step) 生效
                val steppedValue = min + ((it - min) / step).roundToInt() * step
                onValueChange(steppedValue)
            },
            valueRange = min..max,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = BrandBlue,
                inactiveTrackColor = ControlBg
            )
        )

        // 加号按钮
        IconButton(
            onClick = { onValueChange(min(max, value + step)) },
            modifier = Modifier.size(32.dp).clip(CircleShape).background(ControlBg)
        ) { Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = TextMuted, modifier = Modifier.size(18.dp)) }

        // 数值显示
        val displayStr = displayValue ?: if (step >= 1f) value.roundToInt().toString() else String.format(Locale.getDefault(), "%.1f", value)
        Text(
            text = displayStr, fontSize = 14.sp, color = TextMuted,
            textAlign = TextAlign.End, modifier = Modifier.width(36.dp).padding(start = 8.dp)
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ControlBg)
            .padding(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) BrandBlue else Color.Transparent)
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextMuted
                )
            }
        }
    }
}

@Composable
fun ListItemArrow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = TextDark)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 13.sp, color = TextMuted)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.padding(start = 4.dp).size(18.dp))
        }
    }
}