package io.legado.app.ui.book.read

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.model.ReadBook
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.theme.legadoComponentTokens
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.postEvent
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import io.legado.app.ui.font.FontSelectDialog
import io.legado.app.ui.book.read.config.TipConfigDialog
import io.legado.app.ui.book.read.config.BgTextConfigDialog
import io.legado.app.ui.book.read.page.provider.ChapterProvider

// --- 品牌色系定义 (用户偏好) ---
val BrandBlue = Color(0xFF4A5B7C)
val IslandBg = Color(0xFFE4E9ED)
val ControlBg = Color(0x66CBD5E1) // 类似 slate-300/40
val TextDark = Color(0xFF334155) // slate-700
val TextMuted = Color(0xFF64748B) // slate-500

/**
 * 阅读界面 Compose 风格二级配置对话框
 */
class ReadInterfaceComposeDialog : BaseDialogFragment(0), FontSelectDialog.CallBack {

    override fun selectFont(path: String) {
        ReadBookConfig.textFont = path
        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
    }

    override val curFontPath: String
        get() = ReadBookConfig.textFont

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(android.R.color.transparent)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            attributes = attr
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                LegadoComposeTheme {
                    ReadInterfaceContent(
                        fragmentManager = childFragmentManager,
                        onDismiss = { dismissAllowingStateLoss() }
                    )
                }
            }
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? ReadBookActivity)?.bottomDialog?.let {
            (activity as ReadBookActivity).bottomDialog++
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ReadBookConfig.save()
        (activity as? ReadBookActivity)?.bottomDialog?.let {
            (activity as ReadBookActivity).bottomDialog--
        }
    }
}

@Composable
fun ReadInterfaceContent(
    fragmentManager: androidx.fragment.app.FragmentManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("font") }
    
    // 品牌色应用逻辑
    val accentColor = Color(context.legadoComponentTokens().shared.accent)
    val brandThemeColor = if (accentColor != Color.Unspecified) accentColor else BrandBlue

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(IslandBg)
                .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
        ) {
            // --- 浮岛内部可滚动内容区 ---
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.6f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (activeTab) {
                    "font" -> FontTabContent(brandThemeColor, fragmentManager)
                    "margins" -> MarginsTabContent(brandThemeColor)
                    "info" -> InfoTabContent(brandThemeColor, fragmentManager)
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
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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
                                    indication = null
                                ) { activeTab = id },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) brandThemeColor else TextMuted,
                                    modifier = Modifier.size(20.dp).scale(if (isSelected) 1f else 0.95f)
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) brandThemeColor else TextMuted,
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

// ================= Tab 内容组件 =================

@Composable
fun FontTabContent(accentColor: Color, fragmentManager: androidx.fragment.app.FragmentManager) {
    var chineseType by remember { mutableIntStateOf(AppConfig.chineseConverterType) }
    var textBold by remember { mutableIntStateOf(ReadBookConfig.textBold) }
    var textSize by remember { mutableIntStateOf(ReadBookConfig.textSize) }
    var letterSpacing by remember { mutableFloatStateOf(ReadBookConfig.letterSpacing) }
    var lineSpacing by remember { mutableIntStateOf(ReadBookConfig.lineSpacingExtra) }
    var paragraphSpacing by remember { mutableIntStateOf(ReadBookConfig.paragraphSpacing) }
    var pageAnim by remember { mutableIntStateOf(ReadBookConfig.pageAnim) }
    var shareLayout by remember { mutableStateOf(ReadBookConfig.shareLayout) }
    var activeStyle by remember { mutableIntStateOf(ReadBookConfig.styleSelect) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // 1. 字体
        SectionGroup("排版风格") {
            ListItemArrow("选择字体", ReadBookConfig.textFont.ifBlank { "系统默认" }) {
                FontSelectDialog().show(fragmentManager, "fontSelect")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WeightSelector(
                    textBold = textBold,
                    onWeightSelected = {
                        textBold = it
                        ReadBookConfig.textBold = it
                        postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
                    },
                    accentColor = accentColor
                )

                SegmentedControl(
                    options = listOf("简", "繁"),
                    selectedOption = if (chineseType == 2) "繁" else "简",
                    onOptionSelected = { 
                        val type = if (it == "繁") 2 else 1
                        chineseType = type
                        AppConfig.chineseConverterType = type
                        postEvent(EventBus.UP_CONFIG, arrayListOf(5))
                    },
                    accentColor = accentColor,
                    modifier = Modifier.width(100.dp)
                )
            }
        }

        // 2. 详细参数
        SectionGroup("参数调节") {
            SettingSlider("字号", textSize.toFloat(), 5f, 50f, 1f, {
                textSize = it.toInt()
                ReadBookConfig.textSize = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor)
            
            SettingSlider("字距", letterSpacing, -0.5f, 1.0f, 0.05f, {
                letterSpacing = it
                ReadBookConfig.letterSpacing = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor)
            
            SettingSlider("行距", lineSpacing.toFloat(), 0f, 50f, 1f, {
                lineSpacing = it.toInt()
                ReadBookConfig.lineSpacingExtra = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor)
            
            SettingSlider("段距", paragraphSpacing.toFloat(), 0f, 50f, 1f, {
                paragraphSpacing = it.toInt()
                ReadBookConfig.paragraphSpacing = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor)
        }

        // 3. 翻页动画
        SectionGroup("翻页动画") {
            val anims = listOf("覆盖", "滑动", "仿真", "滚屏", "无")
            val animValues = listOf(0, 1, 2, 3, 4)
            SegmentedControl(
                options = anims,
                selectedOption = when(pageAnim) {
                    0 -> "覆盖"
                    1 -> "滑动"
                    2 -> "仿真"
                    3 -> "滚屏"
                    else -> "无"
                },
                onOptionSelected = {
                    val idx = anims.indexOf(it)
                    val value = animValues[idx]
                    pageAnim = value
                    ReadBook.book?.setPageAnim(-1)
                    ReadBookConfig.pageAnim = value
                    postEvent(EventBus.UP_CONFIG, arrayListOf(5))
                    postEvent(EventBus.NOTIFY_MAIN, "")
                },
                modifier = Modifier.fillMaxWidth(),
                accentColor = accentColor
            )
        }

        // 4. 色彩主题
        SectionGroup(title = "色彩方案", action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("共用布局", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = shareLayout,
                    onCheckedChange = {
                        shareLayout = it
                        ReadBookConfig.shareLayout = it
                        postEvent(EventBus.UP_CONFIG, arrayListOf(1, 2, 5))
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                )
            }
        }) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(ReadBookConfig.configList) { index, style ->
                    val isSelected = activeStyle == index
                    val bgStyleColor = Color(android.graphics.Color.parseColor(style.curBgStr()))
                    val textStyleColor = Color(style.curTextColor())
                    
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(bgStyleColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accentColor else Color.Black.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .clickable {
                                activeStyle = index
                                ReadBookConfig.styleSelect = index
                                postEvent(EventBus.UP_CONFIG, arrayListOf(1, 2, 5))
                                if (AppConfig.readBarStyleFollowPage) {
                                    postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (style.name.isNotBlank()) style.name.take(1) else "A",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textStyleColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeightSelector(
    textBold: Int,
    onWeightSelected: (Int) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ControlBg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val options = listOf(
            Triple(2, "细", FontWeight.Light),
            Triple(0, "中", FontWeight.Normal),
            Triple(1, "粗", FontWeight.Bold)
        )
        options.forEach { (value, label, weight) ->
            val isSelected = textBold == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable { onWeightSelected(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aa",
                    fontSize = 13.sp,
                    fontWeight = weight,
                    color = if (isSelected) Color.White else TextMuted
                )
            }
        }
    }
}

@Composable
fun MarginsTabContent(accentColor: Color) {
    var headerShowLine by remember { mutableStateOf(ReadBookConfig.showHeaderLine) }
    var footerShowLine by remember { mutableStateOf(ReadBookConfig.showFooterLine) }
    
    // 正文边距
    var paddingT by remember { mutableIntStateOf(ReadBookConfig.paddingTop) }
    var paddingB by remember { mutableIntStateOf(ReadBookConfig.paddingBottom) }
    var paddingL by remember { mutableIntStateOf(ReadBookConfig.paddingLeft) }
    var paddingR by remember { mutableIntStateOf(ReadBookConfig.paddingRight) }
    
    // 页眉边距
    var hPaddingT by remember { mutableIntStateOf(ReadBookConfig.headerPaddingTop) }
    var hPaddingB by remember { mutableIntStateOf(ReadBookConfig.headerPaddingBottom) }
    var hPaddingL by remember { mutableIntStateOf(ReadBookConfig.headerPaddingLeft) }
    var hPaddingR by remember { mutableIntStateOf(ReadBookConfig.headerPaddingRight) }
 
    // 页脚边距
    var fPaddingT by remember { mutableIntStateOf(ReadBookConfig.footerPaddingTop) }
    var fPaddingB by remember { mutableIntStateOf(ReadBookConfig.footerPaddingBottom) }
    var fPaddingL by remember { mutableIntStateOf(ReadBookConfig.footerPaddingLeft) }
    var fPaddingR by remember { mutableIntStateOf(ReadBookConfig.footerPaddingRight) }

    var tipSize by remember { mutableIntStateOf(ReadBookConfig.tipSize) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SectionGroup(title = "页眉", action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("分隔线", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = headerShowLine,
                    onCheckedChange = {
                        headerShowLine = it
                        ReadBookConfig.showHeaderLine = it
                        postEvent(EventBus.UP_CONFIG, arrayListOf(2))
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                )
            }
        }) {
            SettingSlider("字号", tipSize.toFloat(), 5f, 30f, 1f, {
                tipSize = it.toInt()
                ReadBookConfig.tipSize = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor = accentColor)
            SettingSlider("上", hPaddingT.toFloat(), 0f, 100f, 1f, {
                hPaddingT = it.toInt()
                ReadBookConfig.headerPaddingTop = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor = accentColor)
            SettingSlider("下", hPaddingB.toFloat(), 0f, 100f, 1f, {
                hPaddingB = it.toInt()
                ReadBookConfig.headerPaddingBottom = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor = accentColor)
            SettingSlider("左", hPaddingL.toFloat(), 0f, 100f, 1f, {
                hPaddingL = it.toInt()
                ReadBookConfig.headerPaddingLeft = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor = accentColor)
            SettingSlider("右", hPaddingR.toFloat(), 0f, 100f, 1f, {
                hPaddingR = it.toInt()
                ReadBookConfig.headerPaddingRight = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor = accentColor)
        }

        SectionGroup(title = "页脚", action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("分隔线", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(end = 8.dp))
                Switch(
                    checked = footerShowLine,
                    onCheckedChange = {
                        footerShowLine = it
                        ReadBookConfig.showFooterLine = it
                        postEvent(EventBus.UP_CONFIG, arrayListOf(2))
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                )
            }
        }) {
            SettingSlider("上", fPaddingT.toFloat(), 0f, 100f, 1f, {
                fPaddingT = it.toInt()
                ReadBookConfig.footerPaddingTop = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor = accentColor)
            SettingSlider("下", fPaddingB.toFloat(), 0f, 100f, 1f, {
                fPaddingB = it.toInt()
                ReadBookConfig.footerPaddingBottom = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor = accentColor)
            SettingSlider("左", fPaddingL.toFloat(), 0f, 100f, 1f, {
                fPaddingL = it.toInt()
                ReadBookConfig.footerPaddingLeft = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor = accentColor)
            SettingSlider("右", fPaddingR.toFloat(), 0f, 100f, 1f, {
                fPaddingR = it.toInt()
                ReadBookConfig.footerPaddingRight = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor = accentColor)
        }

        SectionGroup("正文距边缘") {
            SettingSlider("上", paddingT.toFloat(), 0f, 100f, 1f, {
                paddingT = it.toInt()
                ReadBookConfig.paddingTop = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
            }, accentColor = accentColor)
            SettingSlider("下", paddingB.toFloat(), 0f, 100f, 1f, {
                paddingB = it.toInt()
                ReadBookConfig.paddingBottom = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
            }, accentColor = accentColor)
            SettingSlider("左", paddingL.toFloat(), 0f, 100f, 1f, {
                paddingL = it.toInt()
                ReadBookConfig.paddingLeft = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
            }, accentColor = accentColor)
            SettingSlider("右", paddingR.toFloat(), 0f, 100f, 1f, {
                paddingR = it.toInt()
                ReadBookConfig.paddingRight = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
            }, accentColor = accentColor)
        }
    }
}

@Composable
fun InfoTabContent(accentColor: Color, fragmentManager: androidx.fragment.app.FragmentManager) {
    var titleMode by remember { mutableIntStateOf(ReadBookConfig.titleMode) }
    var titleSize by remember { mutableIntStateOf(ReadBookConfig.titleSize) }
    var titleTop by remember { mutableIntStateOf(ReadBookConfig.titleTopSpacing) }
    var titleBottom by remember { mutableIntStateOf(ReadBookConfig.titleBottomSpacing) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SectionGroup("正文标题") {
            SegmentedControl(
                options = listOf("靠左", "居中", "隐藏"),
                selectedOption = when(titleMode) {
                    1 -> "居中"
                    2 -> "隐藏"
                    else -> "靠左"
                },
                onOptionSelected = {
                    val mode = when(it) {
                        "居中" -> 1
                        "隐藏" -> 2
                        else -> 0
                    }
                    titleMode = mode
                    ReadBookConfig.titleMode = mode
                    postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                accentColor = accentColor
            )
            SettingSlider("字号", titleSize.toFloat(), 10f, 60f, 1f, {
                titleSize = it.toInt()
                ReadBookConfig.titleSize = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor)
            SettingSlider("上边距", titleTop.toFloat(), 0f, 100f, 1f, {
                titleTop = it.toInt()
                ReadBookConfig.titleTopSpacing = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor)
            SettingSlider("下边距", titleBottom.toFloat(), 0f, 100f, 1f, {
                titleBottom = it.toInt()
                ReadBookConfig.titleBottomSpacing = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor)
        }
        
        SectionGroup("界面辅助") {
            ListItemArrow("更多页眉设置", "状态栏相关") {
                TipConfigDialog().show(fragmentManager, "tipConfig")
            }
            ListItemArrow("显示隐藏", "自定义布局") {
                // 辅助线/网格等
            }
        }
    }
}

// ================= 基础原子组件 (对齐用户手改 UI) =================

@Composable
fun SectionGroup(
    title: String, 
    action: (@Composable () -> Unit)? = null, 
    content: @Composable ColumnScope.() -> Unit
) {
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
    onValueChange: (Float) -> Unit, 
    displayValue: String? = null,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 14.sp, color = TextDark, modifier = Modifier.width(56.dp))
        
        IconButton(
            onClick = { onValueChange(max(min, value - step)) },
            modifier = Modifier.size(32.dp).clip(CircleShape).background(ControlBg)
        ) { Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = TextMuted, modifier = Modifier.size(18.dp)) }

        Slider(
            value = value,
            onValueChange = {
                val steppedValue = min + ((it - min) / step).roundToInt() * step
                onValueChange(steppedValue.coerceIn(min, max))
            },
            valueRange = min..max,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = ControlBg
            )
        )

        IconButton(
            onClick = { onValueChange(min(max, value + step)) },
            modifier = Modifier.size(32.dp).clip(CircleShape).background(ControlBg)
        ) { Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = TextMuted, modifier = Modifier.size(18.dp)) }

        val displayStr = displayValue ?: if (step >= 1f) value.roundToInt().toString() else String.format(Locale.getDefault(), "%.1f", value)
        Text(
            text = displayStr, fontSize = 14.sp, color = TextMuted,
            textAlign = TextAlign.End, modifier = Modifier.width(36.dp).padding(start = 8.dp)
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<String>, 
    selectedOption: String, 
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color
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
                    .background(if (isSelected) accentColor else Color.Transparent)
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
fun ListItemArrow(label: String, value: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
