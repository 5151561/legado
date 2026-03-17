package io.legado.app.ui.book.read

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.constant.EventBus
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.theme.legadoComponentTokens
import io.legado.app.utils.postEvent
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
internal fun ReadInterfaceSubMenu(
    textColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("font") }
    // 子级选择状态：null 表示主内容，非 null 为选择项 ID (如 "headerLeft")
    var subSelection by remember { mutableStateOf<String?>(null) }
    
    val accentColor = Color(context.legadoComponentTokens().shared.accent)
    val brandThemeColor = if (accentColor != Color.Unspecified) accentColor else textColor
    val controlBg = textColor.copy(alpha = 0.1f)
    val textMuted = textColor.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize()
    ) {
        // --- 内容区 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 400.dp)
        ) {
            if (subSelection == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (activeTab) {
                        "font" -> FontTabContent(brandThemeColor, textColor, controlBg, textMuted)
                        "margins" -> MarginsTabContent(brandThemeColor, textColor, controlBg, textMuted)
                        "info" -> InfoTabContent(brandThemeColor, textColor, controlBg, textMuted, onOpenSub = { subSelection = it })
                    }
                }
            } else {
                SubSelectionContent(
                    selectionId = subSelection!!,
                    textColor = textColor,
                    accentColor = brandThemeColor,
                    controlBg = controlBg,
                    onBack = { subSelection = null }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // --- 底部导航 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = controlBg,
                modifier = Modifier.size(52.dp),
                onClick = if (subSelection != null) { { subSelection = null } } else onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.padding(14.dp),
                    tint = textColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Tabs
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(controlBg)
                    .padding(4.dp)
                    .alpha(if (subSelection == null) 1f else 0.5f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Triple("font", "字体", Icons.Rounded.TextFields),
                    Triple("margins", "边距", Icons.AutoMirrored.Rounded.ViewQuilt),
                    Triple("info", "信息", Icons.AutoMirrored.Rounded.FormatAlignLeft)
                )

                tabs.forEach { (id, label, icon) ->
                    val isSelected = activeTab == id && subSelection == null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) textColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable(
                                enabled = subSelection == null,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { activeTab = id },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) brandThemeColor else textMuted,
                                modifier = Modifier.size(20.dp).scale(if (isSelected) 1f else 0.95f)
                            )
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) brandThemeColor else textMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontTabContent(accentColor: Color, textColor: Color, controlBg: Color, textMuted: Color) {
    val context = LocalContext.current
    var chineseType by remember { mutableIntStateOf(AppConfig.chineseConverterType) }
    var textBold by remember { mutableIntStateOf(ReadBookConfig.textBold) }
    var textSize by remember { mutableIntStateOf(ReadBookConfig.textSize) }
    var letterSpacing by remember { mutableFloatStateOf(ReadBookConfig.letterSpacing) }
    var lineSpacing by remember { mutableIntStateOf(ReadBookConfig.lineSpacingExtra) }
    var paragraphSpacing by remember { mutableIntStateOf(ReadBookConfig.paragraphSpacing) }
    var pageAnim by remember { mutableIntStateOf(ReadBookConfig.pageAnim) }
    var shareLayout by remember { mutableStateOf(ReadBookConfig.shareLayout) }
    var activeStyle by remember { mutableIntStateOf(ReadBookConfig.styleSelect) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionGroup("排版风格", textColor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 粗细调节 (Aa 图标)
                Box(modifier = Modifier.weight(0.48f)) {
                    BoldnessControl(
                        selectedValue = textBold,
                        onSelected = {
                            textBold = it
                            ReadBookConfig.textBold = it
                            postEvent(EventBus.UP_CONFIG, arrayListOf(8, 9, 6))
                        },
                        accentColor = accentColor,
                        controlBg = controlBg,
                        textColor = textColor
                    )
                }

                // 简繁转换 (分段选择)
                Box(modifier = Modifier.weight(0.52f)) {
                    SubSegmentedControl(
                        options = listOf("简", "繁"),
                        selectedOption = if (chineseType == 2) "繁" else "简",
                        onOptionSelected = { 
                            val type = if (it == "繁") 2 else 1
                            chineseType = type
                            AppConfig.chineseConverterType = type
                            postEvent(EventBus.UP_CONFIG, arrayListOf(5))
                        },
                        accentColor = accentColor,
                        controlBg = controlBg,
                        textMuted = textMuted
                    )
                }
            }
            
            val fm = (context as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
            SubListItemArrow("中文字体", if (ReadBookConfig.textFont.isEmpty()) "系统默认" else ReadBookConfig.textFont, textColor, textMuted) {
                fm?.let { 
                    io.legado.app.ui.font.FontSelectDialog().show(it, "fontSelect")
                }
            }
            SubListItemArrow("英文字体", "系统默认", textColor, textMuted)
        }

        SectionGroup("详细参数", textColor) {
            SubSettingSlider("字号", textSize.toFloat(), 5f, 50f, 1f, {
                textSize = it.toInt()
                ReadBookConfig.textSize = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor, textColor = textColor, controlBg = controlBg, textMuted = textMuted)
            
            SubSettingSlider("字距", letterSpacing, -0.5f, 1.0f, 0.05f, {
                letterSpacing = it
                ReadBookConfig.letterSpacing = it
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor, textColor = textColor, controlBg = controlBg, textMuted = textMuted)
            
            SubSettingSlider("行距", lineSpacing.toFloat(), 0f, 50f, 1f, {
                lineSpacing = it.toInt()
                ReadBookConfig.lineSpacingExtra = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor, textColor = textColor, controlBg = controlBg, textMuted = textMuted)
            
            SubSettingSlider("段距", paragraphSpacing.toFloat(), 0f, 50f, 1f, {
                paragraphSpacing = it.toInt()
                ReadBookConfig.paragraphSpacing = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5))
            }, accentColor = accentColor, textColor = textColor, controlBg = controlBg, textMuted = textMuted)
        }

        SectionGroup("翻页动画", textColor) {
            val anims = listOf("覆盖", "滑动", "仿真", "滚屏", "无")
            SubSegmentedControl(
                options = anims,
                selectedOption = anims.getOrElse(pageAnim.coerceIn(0, 4)) { "无" },
                onOptionSelected = {
                    val idx = anims.indexOf(it)
                    pageAnim = idx
                    ReadBook.book?.setPageAnim(-1)
                    ReadBookConfig.pageAnim = idx
                    postEvent(EventBus.UP_CONFIG, arrayListOf(12, 5))
                },
                modifier = Modifier.fillMaxWidth(),
                accentColor = accentColor,
                controlBg = controlBg,
                textMuted = textMuted
            )
        }

        SectionGroup(title = "色彩方案", textColor = textColor, action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("共用布局", fontSize = 12.sp, color = textMuted, modifier = Modifier.padding(end = 8.dp))
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(bgStyleColor)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accentColor else textColor.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .clickable {
                                activeStyle = index
                                ReadBookConfig.styleSelect = index
                                postEvent(EventBus.UP_CONFIG, arrayListOf(1, 2, 5))
                                postEvent(EventBus.UPDATE_READ_ACTION_BAR, true)
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
private fun BoldnessControl(
    selectedValue: Int,
    onSelected: (Int) -> Unit,
    accentColor: Color,
    controlBg: Color,
    textColor: Color
) {
    // 0:正常, 1:粗体, 2:细体
    val options = listOf(2, 0, 1)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(controlBg)
            .padding(4.dp)
    ) {
        options.forEach { value ->
            val isSelected = value == selectedValue
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) accentColor else Color.Transparent)
                    .clickable { onSelected(value) }
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aa",
                    fontSize = 13.sp,
                    fontWeight = when(value) {
                        2 -> FontWeight.Thin
                        0 -> FontWeight.Normal
                        else -> FontWeight.Black
                    },
                    color = if (isSelected) Color.White else textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MarginsTabContent(accentColor: Color, textColor: Color, controlBg: Color, textMuted: Color) {
    var headerShowLine by remember { mutableStateOf(ReadBookConfig.showHeaderLine) }
    var footerShowLine by remember { mutableStateOf(ReadBookConfig.showFooterLine) }
    var tipSize by remember { mutableIntStateOf(ReadBookConfig.tipSize) }
    
    // 页眉边距
    var hT by remember { mutableIntStateOf(ReadBookConfig.headerPaddingTop) }
    var hB by remember { mutableIntStateOf(ReadBookConfig.headerPaddingBottom) }
    var hL by remember { mutableIntStateOf(ReadBookConfig.headerPaddingLeft) }
    var hR by remember { mutableIntStateOf(ReadBookConfig.headerPaddingRight) }

    // 正文边距
    var pT by remember { mutableIntStateOf(ReadBookConfig.paddingTop) }
    var pB by remember { mutableIntStateOf(ReadBookConfig.paddingBottom) }
    var pL by remember { mutableIntStateOf(ReadBookConfig.paddingLeft) }
    var pR by remember { mutableIntStateOf(ReadBookConfig.paddingRight) }

    // 页脚边距
    var fT by remember { mutableIntStateOf(ReadBookConfig.footerPaddingTop) }
    var fB by remember { mutableIntStateOf(ReadBookConfig.footerPaddingBottom) }
    var fL by remember { mutableIntStateOf(ReadBookConfig.footerPaddingLeft) }
    var fR by remember { mutableIntStateOf(ReadBookConfig.footerPaddingRight) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionGroup("页眉", textColor, action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("显示线", fontSize = 12.sp, color = textMuted, modifier = Modifier.padding(end = 4.dp))
                Checkbox(checked = headerShowLine, onCheckedChange = {
                    headerShowLine = it
                    ReadBookConfig.showHeaderLine = it
                    postEvent(EventBus.UP_CONFIG, arrayListOf(2))
                }, colors = CheckboxDefaults.colors(checkedColor = accentColor))
            }
        }) {
            SubSettingSlider("字号", tipSize.toFloat(), 5f, 30f, 1f, {
                tipSize = it.toInt()
                ReadBookConfig.tipSize = it.toInt()
                postEvent(EventBus.UP_CONFIG, arrayListOf(2))
            }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("上", hT.toFloat(), 0f, 100f, 1f, { hT = it.toInt(); ReadBookConfig.headerPaddingTop = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(2)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("下", hB.toFloat(), 0f, 100f, 1f, { hB = it.toInt(); ReadBookConfig.headerPaddingBottom = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(2)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("左", hL.toFloat(), 0f, 100f, 1f, { hL = it.toInt(); ReadBookConfig.headerPaddingLeft = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(2)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("右", hR.toFloat(), 0f, 100f, 1f, { hR = it.toInt(); ReadBookConfig.headerPaddingRight = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(2)) }, accentColor, textColor, controlBg, textMuted)
        }

        SectionGroup("正文", textColor) {
            SubSettingSlider("上", pT.toFloat(), 0f, 100f, 1f, { pT = it.toInt(); ReadBookConfig.paddingTop = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("下", pB.toFloat(), 0f, 100f, 1f, { pB = it.toInt(); ReadBookConfig.paddingBottom = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("左", pL.toFloat(), 0f, 100f, 1f, { pL = it.toInt(); ReadBookConfig.paddingLeft = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("右", pR.toFloat(), 0f, 100f, 1f, { pR = it.toInt(); ReadBookConfig.paddingRight = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5)) }, accentColor, textColor, controlBg, textMuted)
        }

        SectionGroup("页脚", textColor, action = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("显示线", fontSize = 12.sp, color = textMuted, modifier = Modifier.padding(end = 4.dp))
                Checkbox(checked = footerShowLine, onCheckedChange = {
                    footerShowLine = it
                    ReadBookConfig.showFooterLine = it
                    postEvent(EventBus.UP_CONFIG, arrayListOf(2))
                }, colors = CheckboxDefaults.colors(checkedColor = accentColor))
            }
        }) {
            SubSettingSlider("上", fT.toFloat(), 0f, 100f, 1f, { fT = it.toInt(); ReadBookConfig.footerPaddingTop = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(2)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("下", fB.toFloat(), 0f, 100f, 1f, { fB = it.toInt(); ReadBookConfig.footerPaddingBottom = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(2)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("左", fL.toFloat(), 0f, 100f, 1f, { fL = it.toInt(); ReadBookConfig.footerPaddingLeft = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(2)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("右", fR.toFloat(), 0f, 100f, 1f, { fR = it.toInt(); ReadBookConfig.footerPaddingRight = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(2)) }, accentColor, textColor, controlBg, textMuted)
        }
    }
}

@Composable
private fun InfoTabContent(
    accentColor: Color, 
    textColor: Color, 
    controlBg: Color, 
    textMuted: Color,
    onOpenSub: (String) -> Unit
) {
    val context = LocalContext.current
    var titleMode by remember { mutableIntStateOf(ReadBookConfig.titleMode) }
    var titleSize by remember { mutableIntStateOf(ReadBookConfig.titleSize) }
    var titleTop by remember { mutableIntStateOf(ReadBookConfig.titleTopSpacing) }
    var titleBottom by remember { mutableIntStateOf(ReadBookConfig.titleBottomSpacing) }
    
    val headerModes = remember { io.legado.app.help.config.ReadTipConfig.getHeaderModes(context).values.toList() }
    val footerModes = remember { io.legado.app.help.config.ReadTipConfig.getFooterModes(context).values.toList() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionGroup("正文标题", textColor) {
            SubSegmentedControl(
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
                    postEvent(EventBus.UP_CONFIG, arrayListOf(12, 5))
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                accentColor = accentColor,
                controlBg = controlBg,
                textMuted = textMuted
            )
            SubSettingSlider("字号", titleSize.toFloat(), 10f, 60f, 1f, { titleSize = it.toInt(); ReadBookConfig.titleSize = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("上边距", titleTop.toFloat(), 0f, 100f, 1f, { titleTop = it.toInt(); ReadBookConfig.titleTopSpacing = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5)) }, accentColor, textColor, controlBg, textMuted)
            SubSettingSlider("下边距", titleBottom.toFloat(), 0f, 100f, 1f, { titleBottom = it.toInt(); ReadBookConfig.titleBottomSpacing = it.toInt(); postEvent(EventBus.UP_CONFIG, arrayListOf(8, 5)) }, accentColor, textColor, controlBg, textMuted)
        }

        SectionGroup("页眉", textColor) {
            SubListItemArrow("显示/隐藏", headerModes[ReadBookConfig.config.headerMode.coerceIn(0, 2)], textColor, textMuted) { onOpenSub("headerMode") }
            SubListItemArrow("左", getTipName(ReadBookConfig.config.tipHeaderLeft), textColor, textMuted) { onOpenSub("headerLeft") }
            SubListItemArrow("中", getTipName(ReadBookConfig.config.tipHeaderMiddle), textColor, textMuted) { onOpenSub("headerMiddle") }
            SubListItemArrow("右", getTipName(ReadBookConfig.config.tipHeaderRight), textColor, textMuted) { onOpenSub("headerRight") }
        }

        SectionGroup("页脚", textColor) {
            SubListItemArrow("显示/隐藏", footerModes[ReadBookConfig.config.footerMode.coerceIn(0, 1)], textColor, textMuted) { onOpenSub("footerMode") }
            SubListItemArrow("左", getTipName(ReadBookConfig.config.tipFooterLeft), textColor, textMuted) { onOpenSub("footerLeft") }
            SubListItemArrow("中", getTipName(ReadBookConfig.config.tipFooterMiddle), textColor, textMuted) { onOpenSub("footerMiddle") }
            SubListItemArrow("右", getTipName(ReadBookConfig.config.tipFooterRight), textColor, textMuted) { onOpenSub("footerRight") }
        }
        
        SectionGroup("页眉 & 页脚装饰", textColor) {
            val fm = (context as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
            SubListItemArrow("更多设置", "点击进入", textColor, textMuted) {
                fm?.let { 
                    io.legado.app.ui.book.read.config.TipConfigDialog().show(it, "tipConfig")
                }
            }
        }
    }
}

@Composable
private fun SubSelectionContent(
    selectionId: String,
    textColor: Color,
    accentColor: Color,
    controlBg: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tipNames = remember { io.legado.app.help.config.ReadTipConfig.tipNames }
    val tipValues = remember { io.legado.app.help.config.ReadTipConfig.tipValues }
    
    val headerModes = remember { io.legado.app.help.config.ReadTipConfig.getHeaderModes(context) }
    val footerModes = remember { io.legado.app.help.config.ReadTipConfig.getFooterModes(context) }

    val (title, options, currentVal, onSelect) = when(selectionId) {
        "headerMode" -> Quadruple("页眉显示模式", headerModes.values.toList(), ReadBookConfig.config.headerMode, { v: Int -> ReadBookConfig.config.headerMode = v; postEvent(EventBus.UP_CONFIG, arrayListOf(2)) })
        "footerMode" -> Quadruple("页脚显示模式", footerModes.values.toList(), ReadBookConfig.config.footerMode, { v: Int -> ReadBookConfig.config.footerMode = v; postEvent(EventBus.UP_CONFIG, arrayListOf(2)) })
        "headerLeft" -> Quadruple("页眉左侧内容", tipNames, ReadBookConfig.config.tipHeaderLeft, { v: Int -> ReadBookConfig.config.tipHeaderLeft = tipValues[v]; postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6)) })
        "headerMiddle" -> Quadruple("页眉中间内容", tipNames, ReadBookConfig.config.tipHeaderMiddle, { v: Int -> ReadBookConfig.config.tipHeaderMiddle = tipValues[v]; postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6)) })
        "headerRight" -> Quadruple("页眉右侧内容", tipNames, ReadBookConfig.config.tipHeaderRight, { v: Int -> ReadBookConfig.config.tipHeaderRight = tipValues[v]; postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6)) })
        "footerLeft" -> Quadruple("页脚左侧内容", tipNames, ReadBookConfig.config.tipFooterLeft, { v: Int -> ReadBookConfig.config.tipFooterLeft = tipValues[v]; postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6)) })
        "footerMiddle" -> Quadruple("页脚中间内容", tipNames, ReadBookConfig.config.tipFooterMiddle, { v: Int -> ReadBookConfig.config.tipFooterMiddle = tipValues[v]; postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6)) })
        "footerRight" -> Quadruple("页脚右侧内容", tipNames, ReadBookConfig.config.tipFooterRight, { v: Int -> ReadBookConfig.config.tipFooterRight = tipValues[v]; postEvent(EventBus.UP_CONFIG, arrayListOf(2, 6)) })
        else -> error("unknown selectionId")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = textColor, modifier = Modifier.padding(bottom = 12.dp))
        
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(options) { index, name ->
                val isSelected = if (selectionId.endsWith("Mode")) currentVal == index else tipValues.indexOf(currentVal) == index
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    onClick = { onSelect(index); onBack() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) accentColor else textColor,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun getTipName(value: Int): String {
    val idx = io.legado.app.help.config.ReadTipConfig.tipValues.indexOf(value).coerceAtLeast(0)
    return io.legado.app.help.config.ReadTipConfig.tipNames.getOrElse(idx) { "无" }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun SectionGroup(
    title: String, 
    textColor: Color,
    action: (@Composable () -> Unit)? = null, 
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.9f))
            action?.invoke()
        }
        content()
    }
}

@Composable
private fun SubSettingSlider(
    label: String, value: Float, min: Float, max: Float, step: Float,
    onValueChange: (Float) -> Unit, 
    accentColor: Color,
    textColor: Color,
    controlBg: Color,
    textMuted: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 14.sp, color = textColor, modifier = Modifier.width(48.dp))
        
        IconButton(
            onClick = { onValueChange(max(min, value - step)) },
            modifier = Modifier.size(28.dp)
        ) { Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = textMuted, modifier = Modifier.size(24.dp)) }

        Slider(
            value = value,
            onValueChange = {
                val steppedValue = min + ((it - min) / step).roundToInt() * step
                onValueChange(steppedValue.coerceIn(min, max))
            },
            valueRange = min..max,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = textColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = controlBg
            )
        )

        IconButton(
            onClick = { onValueChange(min(max, value + step)) },
            modifier = Modifier.size(28.dp)
        ) { Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = textMuted, modifier = Modifier.size(24.dp)) }

        Text(
            text = value.toInt().toString(), fontSize = 13.sp, color = textMuted,
            textAlign = TextAlign.End, modifier = Modifier.width(28.dp).padding(start = 4.dp)
        )
    }
}

@Composable
private fun SubSegmentedControl(
    options: List<String>, 
    selectedOption: String, 
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color,
    controlBg: Color,
    textMuted: Color
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(controlBg)
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
                    color = if (isSelected) Color.White else textMuted
                )
            }
        }
    }
}

@Composable
private fun SubListItemArrow(label: String, value: String, textColor: Color, textMuted: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = textColor)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 13.sp, color = textMuted)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = textMuted, modifier = Modifier.padding(start = 4.dp).size(18.dp))
        }
    }
}
