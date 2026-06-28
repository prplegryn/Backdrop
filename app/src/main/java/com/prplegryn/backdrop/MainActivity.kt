package com.prplegryn.backdrop

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.shadow.Shadow
import com.prplegryn.backdrop.components.LiquidBottomTab
import com.prplegryn.backdrop.components.LiquidBottomTabs
import com.prplegryn.backdrop.components.LiquidButton
import com.prplegryn.backdrop.components.LiquidSlider
import com.prplegryn.backdrop.components.LiquidToggle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT

        setContent {
            BackdropGalleryApp()
        }
    }
}

private enum class ControlKind {
    Button,
    TintedButton,
    Toggle,
    Slider,
    BottomTabs,
    BottomBar,
    BottomSheet,
    Dialog,
    Magnifier
}

private data class GlassParams(
    val blur: Float,
    val lensHeight: Float,
    val lensAmount: Float,
    val surfaceAlpha: Float,
    val tintAlpha: Float,
    val corner: Float,
    val shadowAlpha: Float,
    val saturation: Float,
    val chromaticAberration: Boolean
)

private data class ControlSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: ControlKind,
    val accent: Color,
    val defaultParams: GlassParams,
    val defaultBackground: Int = 0
)

private data class BackgroundSpec(
    val name: String,
    val resId: Int
)

private class ControlState(
    initialParams: GlassParams,
    initialBackground: Int
) {
    var params by mutableStateOf(initialParams)
    var backgroundIndex by mutableIntStateOf(initialBackground)
    var backgroundOffset by mutableStateOf(Offset.Zero)
}

private val AppBackground = Color(0xFFF6F7F3)
private val Ink = Color(0xFF151515)
private val MutedInk = Color(0xFF5F666B)
private val Hairline = Color(0x1A111111)
private val Panel = Color.White.copy(alpha = 0.74f)
private val Blue = Color(0xFF0077FF)
private val Green = Color(0xFF34C759)
private val Coral = Color(0xFFFF5F57)

@Composable
private fun BackdropGalleryApp() {
    val controls = remember { controlSpecs() }
    val backgrounds = remember { backgroundSpecs() }
    val states = remember {
        controls.associate { spec ->
            spec.id to ControlState(spec.defaultParams, spec.defaultBackground)
        }
    }
    var selectedId by rememberSaveable { mutableStateOf(controls.first().id) }
    val selectedSpec = controls.first { it.id == selectedId }
    val selectedState = states.getValue(selectedId)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        val wide = maxWidth >= 780.dp

        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header()
            ControlStrip(
                controls = controls,
                selectedId = selectedId,
                onSelect = { selectedId = it }
            )

            if (wide) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PreviewPanel(
                        spec = selectedSpec,
                        state = selectedState,
                        backgrounds = backgrounds,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    ParameterPanel(
                        spec = selectedSpec,
                        state = selectedState,
                        backgrounds = backgrounds,
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                    )
                }
            } else {
                PreviewPanel(
                    spec = selectedSpec,
                    state = selectedState,
                    backgrounds = backgrounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 320.dp)
                )
                ParameterPanel(
                    spec = selectedSpec,
                    state = selectedState,
                    backgrounds = backgrounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 330.dp)
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(
                text = "Backdrop",
                size = 28,
                weight = FontWeight.SemiBold,
                color = Ink
            )
            AppText(
                text = "AndroidLiquidGlass Gallery",
                size = 13,
                color = MutedInk
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE9F4EE))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            AppText("2.0.0", size = 12, weight = FontWeight.Medium, color = Color(0xFF27694B))
        }
    }
}

@Composable
private fun ControlStrip(
    controls: List<ControlSpec>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        controls.forEach { spec ->
            PillButton(
                label = spec.title,
                selected = spec.id == selectedId,
                accent = spec.accent,
                onClick = { onSelect(spec.id) }
            )
        }
    }
}

@Composable
private fun PreviewPanel(
    spec: ControlSpec,
    state: ControlState,
    backgrounds: List<BackgroundSpec>,
    modifier: Modifier = Modifier
) {
    val backdrop = rememberLayerBackdrop {
        drawRect(Color.White)
        drawContent()
    }

    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Hairline, RoundedCornerShape(28.dp))
            .pointerInput(state) {
                detectDragGestures { _, dragAmount ->
                    state.backgroundOffset += dragAmount
                }
            }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .layerBackdrop(backdrop)
        ) {
            MovableBackground(
                background = backgrounds[state.backgroundIndex],
                offset = state.backgroundOffset
            )
            PreviewBackdropContent(spec.accent)
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            ShowcaseControl(
                spec = spec,
                params = state.params,
                backdrop = backdrop
            )
        }

        AppText(
            text = spec.subtitle,
            size = 12,
            weight = FontWeight.Medium,
            color = Ink,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
        )
    }
}

@Composable
private fun MovableBackground(
    background: BackgroundSpec,
    offset: Offset
) {
    Image(
        painter = painterResource(background.resId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offset.x
                translationY = offset.y
                scaleX = 1.16f
                scaleY = 1.16f
            }
    )
}

@Composable
private fun PreviewBackdropContent(accent: Color) {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color.White.copy(alpha = 0.08f))
        drawCircle(
            color = accent.copy(alpha = 0.22f),
            radius = size.minDimension * 0.38f,
            center = Offset(size.width * 0.82f, size.height * 0.14f)
        )
        drawCircle(
            color = Coral.copy(alpha = 0.14f),
            radius = size.minDimension * 0.30f,
            center = Offset(size.width * 0.12f, size.height * 0.88f)
        )
        val step = 42.dp.toPx()
        var x = -step
        while (x < size.width + step) {
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(x, 0f),
                end = Offset(x + size.height * 0.45f, size.height),
                strokeWidth = 1.dp.toPx()
            )
            x += step
        }
    }
}

@Composable
private fun ShowcaseControl(
    spec: ControlSpec,
    params: GlassParams,
    backdrop: Backdrop
) {
    when (spec.kind) {
        ControlKind.Button -> OfficialButtons(backdrop = backdrop, params = params)

        ControlKind.TintedButton -> OfficialTintedButtons(backdrop = backdrop, params = params)

        ControlKind.Toggle -> OfficialToggle(backdrop = backdrop, params = params)

        ControlKind.Slider -> OfficialSlider(backdrop = backdrop, params = params)

        ControlKind.BottomTabs -> OfficialBottomTabs(backdrop = backdrop, params = params)

        ControlKind.BottomBar -> DemoBottomBar(
            backdrop = backdrop,
            params = params,
            accent = spec.accent
        )

        ControlKind.BottomSheet -> DemoBottomSheet(
            backdrop = backdrop,
            params = params,
            accent = spec.accent
        )

        ControlKind.Dialog -> DemoDialog(
            backdrop = backdrop,
            params = params,
            accent = spec.accent
        )

        ControlKind.Magnifier -> DemoMagnifier(
            backdrop = backdrop,
            params = params
        )
    }
}

@Composable
private fun OfficialButtons(backdrop: Backdrop, params: GlassParams) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LiquidButton(
            {},
            backdrop,
            blurRadius = params.blur,
            lensHeight = params.lensHeight,
            lensAmount = params.lensAmount
        ) {
            AppText("Transparent Liquid Button", size = 15, color = Ink)
        }
        LiquidButton(
            {},
            backdrop,
            surfaceColor = Color.White.copy(alpha = params.surfaceAlpha),
            blurRadius = params.blur,
            lensHeight = params.lensHeight,
            lensAmount = params.lensAmount
        ) {
            AppText("Surface Liquid Button", size = 15, color = Ink)
        }
    }
}

@Composable
private fun OfficialTintedButtons(backdrop: Backdrop, params: GlassParams) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LiquidButton(
            {},
            backdrop,
            tint = Color(0xFF0088FF),
            blurRadius = params.blur,
            lensHeight = params.lensHeight,
            lensAmount = params.lensAmount,
            tintAlpha = params.tintAlpha
        ) {
            AppText("Tinted Liquid Button", size = 15, color = Color.White)
        }
        LiquidButton(
            {},
            backdrop,
            tint = Color(0xFFFF8D28),
            blurRadius = params.blur,
            lensHeight = params.lensHeight,
            lensAmount = params.lensAmount,
            tintAlpha = params.tintAlpha
        ) {
            AppText("Tinted Liquid Button", size = 15, color = Color.White)
        }
    }
}

@Composable
private fun OfficialToggle(backdrop: Backdrop, params: GlassParams) {
    var selected by rememberSaveable { mutableStateOf(false) }
    LiquidToggle(
        selected = { selected },
        onSelect = { selected = it },
        backdrop = backdrop,
        modifier = Modifier.padding(horizontal = 32.dp),
        blurRadius = params.blur,
        lensHeight = params.lensHeight,
        lensAmount = params.lensAmount,
        surfaceAlpha = params.surfaceAlpha,
        chromaticAberration = params.chromaticAberration
    )
}

@Composable
private fun OfficialSlider(backdrop: Backdrop, params: GlassParams) {
    var value by rememberSaveable { mutableFloatStateOf(50f) }
    LiquidSlider(
        value = { value },
        onValueChange = { value = it },
        valueRange = 0f..100f,
        visibilityThreshold = 0.01f,
        backdrop = backdrop,
        modifier = Modifier.padding(horizontal = 32.dp),
        blurRadius = params.blur,
        lensHeight = params.lensHeight,
        lensAmount = params.lensAmount,
        surfaceAlpha = params.surfaceAlpha,
        chromaticAberration = params.chromaticAberration
    )
}

@Composable
private fun OfficialBottomTabs(backdrop: Backdrop, params: GlassParams) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    LiquidBottomTabs(
        selectedTabIndex = { selectedTabIndex },
        onTabSelected = { selectedTabIndex = it },
        backdrop = backdrop,
        tabsCount = 3,
        modifier = Modifier
            .widthIn(max = 430.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        blurRadius = params.blur,
        containerLensHeight = params.lensHeight,
        containerLensAmount = params.lensAmount,
        selectorLensHeight = (params.lensHeight * 10f / 24f).coerceAtLeast(0f),
        selectorLensAmount = (params.lensAmount * 14f / 24f).coerceAtLeast(0f),
        surfaceAlpha = params.surfaceAlpha,
        chromaticAberration = params.chromaticAberration
    ) {
        repeat(3) { index ->
            LiquidBottomTab({ selectedTabIndex = index }) {
                AppText("Tab ${index + 1}", size = 12, color = Ink)
            }
        }
    }
}

@Composable
private fun DemoButton(
    label: String,
    backdrop: Backdrop,
    params: GlassParams,
    accent: Color,
    tinted: Boolean = false
) {
    Row(
        Modifier
            .glassSurface(
                backdrop = backdrop,
                params = params,
                shape = RoundedCornerShape(percent = 50),
                tint = if (tinted) accent else Color.Unspecified
            )
            .height(56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (tinted) Color.White else accent)
        )
        AppText(
            text = label,
            size = 16,
            weight = FontWeight.SemiBold,
            color = if (tinted) Color.White else Ink
        )
    }
}

@Composable
private fun DemoToggle(
    backdrop: Backdrop,
    params: GlassParams,
    accent: Color
) {
    var checked by remember { mutableStateOf(true) }
    Box(
        Modifier
            .glassSurface(
                backdrop = backdrop,
                params = params.copy(surfaceAlpha = params.surfaceAlpha * 0.7f),
                shape = RoundedCornerShape(percent = 50),
                tint = if (checked) accent else Color.Unspecified
            )
            .size(86.dp, 46.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = null,
                indication = null
            ) { checked = !checked }
            .padding(5.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .glassSurface(
                    backdrop = backdrop,
                    params = params.copy(
                        blur = 8f,
                        lensHeight = params.lensHeight * 0.45f,
                        lensAmount = params.lensAmount * 0.45f,
                        surfaceAlpha = 0.92f,
                        tintAlpha = 0f
                    ),
                    shape = RoundedCornerShape(percent = 50)
                )
                .size(36.dp)
        )
    }
}

@Composable
private fun DemoSlider(
    backdrop: Backdrop,
    params: GlassParams,
    accent: Color
) {
    var value by remember { mutableFloatStateOf(0.62f) }
    BoxWithConstraints(
        Modifier
            .widthIn(max = 390.dp)
            .fillMaxWidth()
            .height(74.dp)
            .pointerInput(Unit) {
                fun update(x: Float) {
                    value = (x / size.width.toFloat()).coerceIn(0f, 1f)
                }
                detectTapGestures { offset -> update(offset.x) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    value = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val trackWidthPx = constraints.maxWidth.toFloat()
        val thumbWidthPx = with(density) { 52.dp.toPx() }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(horizontal = 8.dp)
        ) {
            val radius = size.height / 2f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.42f),
                cornerRadius = CornerRadius(radius, radius)
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.88f),
                size = Size(size.width * value, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
        Box(
            Modifier
                .offset {
                    val x = (trackWidthPx * value - thumbWidthPx / 2f)
                        .roundToInt()
                        .coerceIn(0, (trackWidthPx - thumbWidthPx).roundToInt())
                    IntOffset(x, 0)
                }
                .glassSurface(
                    backdrop = backdrop,
                    params = params.copy(
                        surfaceAlpha = (params.surfaceAlpha + 0.28f).coerceAtMost(1f),
                        lensHeight = params.lensHeight * 0.75f,
                        lensAmount = params.lensAmount * 0.6f
                    ),
                    shape = RoundedCornerShape(percent = 50)
                )
                .size(52.dp, 32.dp)
        )
    }
}

@Composable
private fun DemoBottomTabs(
    backdrop: Backdrop,
    params: GlassParams,
    accent: Color
) {
    var selected by remember { mutableIntStateOf(1) }
    val tabs = listOf("Home", "Glass", "Code")

    BoxWithConstraints(
        Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .height(74.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val tabWidth = maxWidth / tabs.size
        Row(
            Modifier
                .glassSurface(
                    backdrop = backdrop,
                    params = params.copy(blur = params.blur.coerceAtLeast(8f)),
                    shape = RoundedCornerShape(percent = 50)
                )
                .fillMaxSize()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) { selected = index },
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = label,
                        size = 13,
                        weight = FontWeight.SemiBold,
                        color = if (selected == index) Color.Transparent else Ink.copy(alpha = 0.72f)
                    )
                }
            }
        }
        Box(
            Modifier
                .offset(x = tabWidth * selected.toFloat())
                .padding(7.dp)
                .width(tabWidth - 2.dp)
                .fillMaxHeight()
                .glassSurface(
                    backdrop = backdrop,
                    params = params.copy(
                        surfaceAlpha = (params.surfaceAlpha + 0.18f).coerceAtMost(1f),
                        tintAlpha = params.tintAlpha * 0.5f
                    ),
                    shape = RoundedCornerShape(percent = 50),
                    tint = accent
                ),
            contentAlignment = Alignment.Center
        ) {
            AppText(
                text = tabs[selected],
                size = 13,
                weight = FontWeight.SemiBold,
                color = Ink
            )
        }
    }
}

@Composable
private fun DemoBottomBar(
    backdrop: Backdrop,
    params: GlassParams,
    accent: Color
) {
    Row(
        Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .height(70.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .glassSurface(
                    backdrop = backdrop,
                    params = params.copy(blur = params.blur.coerceAtLeast(4f)),
                    shape = RoundedCornerShape(percent = 50)
                )
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Feed", "Search", "Saved").forEachIndexed { index, label ->
                AppText(
                    text = label,
                    size = 13,
                    weight = FontWeight.SemiBold,
                    color = if (index == 1) Ink else Ink.copy(alpha = 0.52f)
                )
            }
        }
        Box(
            Modifier
                .aspectRatio(1f)
                .fillMaxHeight()
                .glassSurface(
                    backdrop = backdrop,
                    params = params,
                    shape = RoundedCornerShape(percent = 50),
                    tint = accent
                ),
            contentAlignment = Alignment.Center
        ) {
            AppText("+", size = 26, weight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
private fun DemoBottomSheet(
    backdrop: Backdrop,
    params: GlassParams,
    accent: Color
) {
    val sheetBackdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .widthIn(max = 470.dp)
                .fillMaxWidth()
                .glassSurface(
                    backdrop = backdrop,
                    params = params.copy(
                        blur = params.blur.coerceAtLeast(4f),
                        lensHeight = params.lensHeight.coerceAtLeast(24f),
                        lensAmount = params.lensAmount.coerceAtLeast(48f)
                    ),
                    shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
                    exportedBackdrop = sheetBackdrop
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Ink.copy(alpha = 0.18f))
                    .align(Alignment.CenterHorizontally)
            )
            AppText("Glass Bottom Sheet", size = 18, weight = FontWeight.SemiBold, color = Ink)
            AppText(
                text = "exportedBackdrop keeps nested glass stable.",
                size = 13,
                color = MutedInk,
                maxLines = 2
            )
            Box(
                Modifier
                    .glassSurface(
                        backdrop = sheetBackdrop,
                        params = params.copy(surfaceAlpha = 0.62f),
                        shape = RoundedCornerShape(percent = 50),
                        tint = accent
                    )
                    .height(54.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AppText("Continue", size = 15, weight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun DemoDialog(
    backdrop: Backdrop,
    params: GlassParams,
    accent: Color
) {
    Column(
        Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .glassSurface(
                backdrop = backdrop,
                params = params,
                shape = RoundedCornerShape(params.corner.dp)
            )
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppText("Dialog Surface", size = 20, weight = FontWeight.SemiBold, color = Ink)
        AppText(
            text = "A compact floating panel using the same backdrop chain.",
            size = 13,
            color = MutedInk,
            maxLines = 3
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.44f))
                    .border(1.dp, Hairline, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                AppText("Cancel", size = 14, weight = FontWeight.SemiBold, color = Ink)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .glassSurface(
                        backdrop = backdrop,
                        params = params.copy(surfaceAlpha = 0.42f),
                        shape = RoundedCornerShape(percent = 50),
                        tint = accent
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppText("Apply", size = 14, weight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun DemoMagnifier(
    backdrop: Backdrop,
    params: GlassParams
) {
    Box(
        Modifier
            .glassSurface(
                backdrop = backdrop,
                params = params.copy(
                    blur = 0f,
                    lensHeight = params.lensHeight.coerceAtLeast(24f),
                    lensAmount = params.lensAmount.coerceAtLeast(46f),
                    surfaceAlpha = params.surfaceAlpha * 0.35f
                ),
                shape = RoundedCornerShape(params.corner.dp)
            )
            .size(164.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText("Aa", size = 46, weight = FontWeight.Bold, color = Ink.copy(alpha = 0.72f))
    }
}

@Composable
private fun ParameterPanel(
    spec: ControlSpec,
    state: ControlState,
    backgrounds: List<BackgroundSpec>,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1200)
            copied = false
        }
    }

    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Panel)
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AppText(spec.title, size = 19, weight = FontWeight.SemiBold, color = Ink)
                AppText(spec.subtitle, size = 12, color = MutedInk)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    label = "Reset",
                    selected = false,
                    accent = spec.accent,
                    onClick = {
                        state.params = spec.defaultParams
                        state.backgroundIndex = spec.defaultBackground
                        state.backgroundOffset = Offset.Zero
                    }
                )
                PillButton(
                    label = if (copied) "Copied" else "Copy",
                    selected = copied,
                    accent = spec.accent,
                    onClick = {
                        clipboard.setText(AnnotatedString(codeFor(spec, state.params)))
                        copied = true
                    }
                )
            }
        }

        SectionLabel("Background")
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            backgrounds.forEachIndexed { index, background ->
                PillButton(
                    label = background.name,
                    selected = index == state.backgroundIndex,
                    accent = spec.accent,
                    onClick = {
                        state.backgroundIndex = index
                        state.backgroundOffset = Offset.Zero
                    }
                )
            }
        }

        SectionLabel("Glass")
        ParamSlider(
            label = "Blur",
            value = state.params.blur,
            range = 0f..18f,
            suffix = "dp",
            accent = spec.accent,
            onValueChange = { state.params = state.params.copy(blur = it) }
        )
        ParamSlider(
            label = "Lens height",
            value = state.params.lensHeight,
            range = 0f..36f,
            suffix = "dp",
            accent = spec.accent,
            onValueChange = { state.params = state.params.copy(lensHeight = it) }
        )
        ParamSlider(
            label = "Lens amount",
            value = state.params.lensAmount,
            range = 0f..64f,
            suffix = "dp",
            accent = spec.accent,
            onValueChange = { state.params = state.params.copy(lensAmount = it) }
        )
        ParamSlider(
            label = "Saturation",
            value = state.params.saturation,
            range = 0.5f..2.2f,
            suffix = "x",
            accent = spec.accent,
            onValueChange = { state.params = state.params.copy(saturation = it) }
        )
        ParamSlider(
            label = "Surface",
            value = state.params.surfaceAlpha,
            range = 0f..1f,
            suffix = "",
            accent = spec.accent,
            onValueChange = { state.params = state.params.copy(surfaceAlpha = it) }
        )
        ParamSlider(
            label = "Tint",
            value = state.params.tintAlpha,
            range = 0f..1f,
            suffix = "",
            accent = spec.accent,
            onValueChange = { state.params = state.params.copy(tintAlpha = it) }
        )
        ParamSlider(
            label = "Corner",
            value = state.params.corner,
            range = 12f..60f,
            suffix = "dp",
            accent = spec.accent,
            onValueChange = { state.params = state.params.copy(corner = it) }
        )
        ParamSlider(
            label = "Shadow",
            value = state.params.shadowAlpha,
            range = 0f..0.24f,
            suffix = "",
            accent = spec.accent,
            onValueChange = { state.params = state.params.copy(shadowAlpha = it) }
        )

        ToggleRow(
            label = "Chromatic",
            checked = state.params.chromaticAberration,
            accent = spec.accent,
            onCheckedChange = {
                state.params = state.params.copy(chromaticAberration = it)
            }
        )

    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(label, size = 13, weight = FontWeight.Medium, color = Ink)
            AppText(value.displayValue(suffix), size = 12, color = MutedInk)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(range) {
                    val thumbRadiusPx = 9.dp.toPx()
                    fun update(x: Float) {
                        val trackWidth = (size.width.toFloat() - thumbRadiusPx * 2f).coerceAtLeast(1f)
                        val fraction = ((x - thumbRadiusPx) / trackWidth).coerceIn(0f, 1f)
                        val next = range.start + (range.endInclusive - range.start) * fraction
                        onValueChange(next.coerceIn(range))
                    }
                    detectTapGestures { offset -> update(offset.x) }
                }
                .pointerInput(range) {
                    val thumbRadiusPx = 9.dp.toPx()
                    detectDragGestures { change, _ ->
                        val trackWidth = (size.width.toFloat() - thumbRadiusPx * 2f).coerceAtLeast(1f)
                        val fraction = ((change.position.x - thumbRadiusPx) / trackWidth).coerceIn(0f, 1f)
                        val next = range.start + (range.endInclusive - range.start) * fraction
                        onValueChange(next.coerceIn(range))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .padding(horizontal = 9.dp)
            ) {
                val radius = size.height / 2f
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.12f),
                    cornerRadius = CornerRadius(radius, radius)
                )
                drawRoundRect(
                    color = accent.copy(alpha = 0.9f),
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = CornerRadius(radius, radius)
                )
            }
            Box(
                Modifier
                    .offset {
                        val thumb = 18.dp.roundToPx()
                        val trackWidth = (constraints.maxWidth - thumb).coerceAtLeast(1)
                        val x = (trackWidth * fraction).roundToInt()
                        IntOffset(x.coerceIn(0, trackWidth), 0)
                    }
                    .size(18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
                    .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(label, size = 13, weight = FontWeight.Medium, color = Ink)
        Box(
            Modifier
                .size(54.dp, 30.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) accent.copy(alpha = 0.82f) else Color.Black.copy(alpha = 0.12f))
                .clickable(
                    interactionSource = null,
                    indication = null
                ) { onCheckedChange(!checked) }
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    AppText(
        text = text.uppercase(),
        size = 11,
        weight = FontWeight.Bold,
        color = MutedInk.copy(alpha = 0.76f)
    )
}

@Composable
private fun PillButton(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) accent else Color.White.copy(alpha = 0.72f))
            .border(1.dp, if (selected) accent.copy(alpha = 0.2f) else Hairline, shape)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = label,
            size = 13,
            weight = FontWeight.SemiBold,
            color = if (selected) Color.White else Ink,
            maxLines = 1
        )
    }
}

@Composable
private fun AppText(
    text: String,
    size: Int,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    fontFamily: FontFamily = FontFamily.Default
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = weight,
            fontFamily = fontFamily,
            lineHeight = (size * 1.25f).sp
        )
    )
}

private fun Modifier.glassSurface(
    backdrop: Backdrop,
    params: GlassParams,
    shape: Shape,
    tint: Color = Color.Unspecified,
    exportedBackdrop: LayerBackdrop? = null
): Modifier {
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            colorControls(saturation = params.saturation)
            if (params.blur > 0f) {
                blur(params.blur.dp.toPx())
            }
            if (params.lensHeight > 0f || params.lensAmount > 0f) {
                lens(
                    refractionHeight = params.lensHeight.dp.toPx(),
                    refractionAmount = params.lensAmount.dp.toPx(),
                    chromaticAberration = params.chromaticAberration
                )
            }
        },
        shadow = {
            if (params.shadowAlpha <= 0f) {
                null
            } else {
                Shadow(
                    radius = 22.dp,
                    color = Color.Black.copy(alpha = params.shadowAlpha)
                )
            }
        },
        exportedBackdrop = exportedBackdrop,
        onDrawSurface = {
            if (tint.isSpecified && params.tintAlpha > 0f) {
                drawRect(tint, blendMode = BlendMode.Hue)
                drawRect(tint.copy(alpha = params.tintAlpha))
            }
            if (params.surfaceAlpha > 0f) {
                drawRect(Color.White.copy(alpha = params.surfaceAlpha))
            }
        }
    )
}

private fun controlSpecs(): List<ControlSpec> {
    val button = GlassParams(
        blur = 2f,
        lensHeight = 12f,
        lensAmount = 24f,
        surfaceAlpha = 0.30f,
        tintAlpha = 0.0f,
        corner = 28f,
        shadowAlpha = 0.08f,
        saturation = 1.5f,
        chromaticAberration = false
    )
    val bar = GlassParams(
        blur = 4f,
        lensHeight = 16f,
        lensAmount = 32f,
        surfaceAlpha = 0.50f,
        tintAlpha = 0.75f,
        corner = 32f,
        shadowAlpha = 0.09f,
        saturation = 1.5f,
        chromaticAberration = false
    )
    val control = GlassParams(
        blur = 8f,
        lensHeight = 10f,
        lensAmount = 14f,
        surfaceAlpha = 1f,
        tintAlpha = 0.36f,
        corner = 24f,
        shadowAlpha = 0.06f,
        saturation = 1.35f,
        chromaticAberration = true
    )

    return listOf(
        ControlSpec("button", "Button", "LiquidButton standard", ControlKind.Button, Blue, button, 0),
        ControlSpec("tinted", "Tinted", "Hue blended button", ControlKind.TintedButton, Color(0xFF8A5CFF), button.copy(tintAlpha = 0.75f), 1),
        ControlSpec("toggle", "Toggle", "LiquidToggle standard", ControlKind.Toggle, Green, control.copy(lensHeight = 5f, lensAmount = 10f), 1),
        ControlSpec("slider", "Slider", "LiquidSlider standard", ControlKind.Slider, Blue, control, 0),
        ControlSpec("tabs", "Tabs", "LiquidBottomTabs standard", ControlKind.BottomTabs, Blue, bar.copy(blur = 8f, lensHeight = 24f, lensAmount = 24f, surfaceAlpha = 0.4f), 2),
        ControlSpec("bar", "Bottom Bar", "Glass bottom bar", ControlKind.BottomBar, Blue, bar, 2),
        ControlSpec("sheet", "Sheet", "Glass bottom sheet", ControlKind.BottomSheet, Coral, bar.copy(lensHeight = 24f, lensAmount = 48f, corner = 44f), 1),
        ControlSpec("dialog", "Dialog", "Floating glass panel", ControlKind.Dialog, Color(0xFF111111), button.copy(blur = 6f, lensHeight = 18f, lensAmount = 30f, corner = 34f), 0),
        ControlSpec("magnifier", "Magnifier", "Lens-only surface", ControlKind.Magnifier, Color(0xFF7A4CFF), button.copy(blur = 0f, lensHeight = 28f, lensAmount = 52f, surfaceAlpha = 0.18f, corner = 48f, chromaticAberration = true), 2)
    )
}

private fun backgroundSpecs(): List<BackgroundSpec> {
    return listOf(
        BackgroundSpec("Wallpaper", R.drawable.wallpaper_light),
        BackgroundSpec("Home", R.drawable.system_home_screen_light),
        BackgroundSpec("Banner", R.drawable.backdrop_banner)
    )
}

private fun codeFor(spec: ControlSpec, params: GlassParams): String {
    val shape = when (spec.kind) {
        ControlKind.BottomSheet -> "RoundedCornerShape(topStart = 44f.dp, topEnd = 44f.dp, bottomStart = 28f.dp, bottomEnd = 28f.dp)"
        ControlKind.Dialog, ControlKind.Magnifier -> "RoundedCornerShape(${params.corner.clean()}f.dp)"
        else -> "RoundedCornerShape(percent = 50)"
    }
    val tint = when (spec.kind) {
        ControlKind.TintedButton, ControlKind.BottomTabs, ControlKind.BottomBar, ControlKind.BottomSheet, ControlKind.Dialog -> "Color(0xFF${spec.accent.toArgbString()})"
        else -> "Color.Unspecified"
    }
    val body = when (spec.kind) {
        ControlKind.Button -> """
Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16f.dp)
) {
    LiquidButton(
        {},
        backdrop,
        blurRadius = ${params.blur.clean()}f,
        lensHeight = ${params.lensHeight.clean()}f,
        lensAmount = ${params.lensAmount.clean()}f
    ) {
        BasicText("Transparent Liquid Button", style = TextStyle(Color.Black, 15f.sp))
    }
    LiquidButton(
        {},
        backdrop,
        surfaceColor = Color.White.copy(${params.surfaceAlpha.clean()}f),
        blurRadius = ${params.blur.clean()}f,
        lensHeight = ${params.lensHeight.clean()}f,
        lensAmount = ${params.lensAmount.clean()}f
    ) {
        BasicText("Surface Liquid Button", style = TextStyle(Color.Black, 15f.sp))
    }
}
""".trimIndent()

        ControlKind.TintedButton -> """
Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16f.dp)
) {
    LiquidButton(
        {},
        backdrop,
        tint = Color(0xFF0088FF),
        blurRadius = ${params.blur.clean()}f,
        lensHeight = ${params.lensHeight.clean()}f,
        lensAmount = ${params.lensAmount.clean()}f,
        tintAlpha = ${params.tintAlpha.clean()}f
    ) {
        BasicText("Tinted Liquid Button", style = TextStyle(Color.White, 15f.sp))
    }
    LiquidButton(
        {},
        backdrop,
        tint = Color(0xFFFF8D28),
        blurRadius = ${params.blur.clean()}f,
        lensHeight = ${params.lensHeight.clean()}f,
        lensAmount = ${params.lensAmount.clean()}f,
        tintAlpha = ${params.tintAlpha.clean()}f
    ) {
        BasicText("Tinted Liquid Button", style = TextStyle(Color.White, 15f.sp))
    }
}
""".trimIndent()

        ControlKind.Toggle -> """
var selected by rememberSaveable { mutableStateOf(false) }
LiquidToggle(
    selected = { selected },
    onSelect = { selected = it },
    backdrop = backdrop,
    modifier = Modifier.padding(horizontal = 32f.dp),
    blurRadius = ${params.blur.clean()}f,
    lensHeight = ${params.lensHeight.clean()}f,
    lensAmount = ${params.lensAmount.clean()}f,
    surfaceAlpha = ${params.surfaceAlpha.clean()}f,
    chromaticAberration = ${params.chromaticAberration}
)
""".trimIndent()

        ControlKind.Slider -> """
var value by rememberSaveable { mutableFloatStateOf(50f) }
LiquidSlider(
    value = { value },
    onValueChange = { value = it },
    valueRange = 0f..100f,
    visibilityThreshold = 0.01f,
    backdrop = backdrop,
    modifier = Modifier.padding(horizontal = 32f.dp),
    blurRadius = ${params.blur.clean()}f,
    lensHeight = ${params.lensHeight.clean()}f,
    lensAmount = ${params.lensAmount.clean()}f,
    surfaceAlpha = ${params.surfaceAlpha.clean()}f,
    chromaticAberration = ${params.chromaticAberration}
)
""".trimIndent()

        ControlKind.BottomTabs -> """
var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
LiquidBottomTabs(
    selectedTabIndex = { selectedTabIndex },
    onTabSelected = { selectedTabIndex = it },
    backdrop = backdrop,
    tabsCount = 3,
    modifier = Modifier.padding(horizontal = 36f.dp),
    blurRadius = ${params.blur.clean()}f,
    containerLensHeight = ${params.lensHeight.clean()}f,
    containerLensAmount = ${params.lensAmount.clean()}f,
    selectorLensHeight = ${(params.lensHeight * 10f / 24f).clean()}f,
    selectorLensAmount = ${(params.lensAmount * 14f / 24f).clean()}f,
    surfaceAlpha = ${params.surfaceAlpha.clean()}f,
    chromaticAberration = ${params.chromaticAberration}
) {
    repeat(3) { index ->
        LiquidBottomTab({ selectedTabIndex = index }) {
            BasicText("Tab ${'$'}{index + 1}", style = TextStyle(Color.Black, 12f.sp))
        }
    }
}
""".trimIndent()

        ControlKind.BottomBar -> """
Row(horizontalArrangement = Arrangement.spacedBy(10f.dp)) {
    Row(
        Modifier
            .weight(1f)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { $shape },
                effects = { ${params.effectCode()} },
                onDrawSurface = { ${params.surfaceCode("Color.Unspecified")} }
            )
            .height(70f.dp)
    ) { /* tab content */ }
    Box(
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { $shape },
                effects = { ${params.effectCode()} },
                onDrawSurface = { ${params.surfaceCode(tint)} }
            )
            .size(70f.dp)
    )
}
""".trimIndent()

        ControlKind.BottomSheet -> """
val sheetBackdrop = rememberLayerBackdrop()
Column(
    Modifier
        .drawBackdrop(
            backdrop = backdrop,
            exportedBackdrop = sheetBackdrop,
            shape = { $shape },
            effects = { ${params.effectCode()} },
            onDrawSurface = { ${params.surfaceCode("Color.Unspecified")} }
        )
        .fillMaxWidth()
) {
    Box(
        Modifier
            .drawBackdrop(
                backdrop = sheetBackdrop,
                shape = { RoundedCornerShape(percent = 50) },
                effects = { ${params.effectCode()} },
                onDrawSurface = { ${params.surfaceCode(tint)} }
            )
            .height(54f.dp)
            .fillMaxWidth()
    )
}
""".trimIndent()

        ControlKind.Dialog -> """
Column(
    Modifier
        .drawBackdrop(
            backdrop = backdrop,
            shape = { $shape },
            effects = { ${params.effectCode()} },
            onDrawSurface = { ${params.surfaceCode("Color.Unspecified")} }
        )
        .padding(22f.dp)
) {
    Text("Dialog Surface")
}
""".trimIndent()

        ControlKind.Magnifier -> """
Box(
    Modifier
        .drawBackdrop(
            backdrop = backdrop,
            shape = { $shape },
            effects = { ${params.copy(blur = 0f).effectCode()} },
            onDrawSurface = { drawRect(Color.White.copy(alpha = ${params.surfaceAlpha.clean()}f)) }
        )
        .size(164f.dp)
)
""".trimIndent()
    }

    return """
@Composable
fun ${spec.title.replace(" ", "")}Sample(backdrop: Backdrop) {
$body
}
""".trimIndent()
}

private fun GlassParams.effectCode(): String {
    val chromatic = if (chromaticAberration) ", chromaticAberration = true" else ""
    return "colorControls(saturation = ${saturation.clean()}f); blur(${blur.clean()}f.dp.toPx()); lens(${lensHeight.clean()}f.dp.toPx(), ${lensAmount.clean()}f.dp.toPx()$chromatic)"
}

private fun GlassParams.surfaceCode(tint: String): String {
    val tintCode =
        if (tint == "Color.Unspecified" || tintAlpha <= 0f) {
            ""
        } else {
            "drawRect($tint, blendMode = BlendMode.Hue); drawRect($tint.copy(alpha = ${tintAlpha.clean()}f)); "
        }
    return "${tintCode}drawRect(Color.White.copy(alpha = ${surfaceAlpha.clean()}f))"
}

private fun Float.displayValue(suffix: String): String {
    val value = clean()
    return if (suffix.isEmpty()) value else "$value$suffix"
}

private fun Float.clean(): String {
    val rounded = (this * 10f).roundToInt() / 10f
    val whole = rounded.roundToInt()
    return if (rounded == whole.toFloat()) whole.toString() else rounded.toString()
}

private fun Color.toArgbString(): String {
    val rgb = toArgb() and 0x00FFFFFF
    return rgb.toString(16).uppercase().padStart(6, '0')
}
